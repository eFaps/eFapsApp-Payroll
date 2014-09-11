/*
 * Copyright 2003 - 2014 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.payroll.rules;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.jexl2.JexlArithmetic;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlInfo;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.jexl2.introspection.JexlMethod;
import org.apache.commons.jexl2.introspection.JexlPropertyGet;
import org.apache.commons.jexl2.introspection.JexlPropertySet;
import org.apache.commons.jexl2.introspection.UberspectImpl;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.payroll.util.Payroll;
import org.efaps.esjp.payroll.util.PayrollSettings;
import org.efaps.esjp.ui.html.Table;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Calculator_Base.java 13971 2014-09-08 21:03:58Z jan@moxter.net
 *          $
 */
@EFapsUUID("a18eb023-438e-442b-853e-3b5b61517ca8")
@EFapsRevision("$Rev$")
public abstract class Calculator_Base
{
    protected static final String PARAKEY4CONTEXT = "Context";

    private static MessageLog MSGLOG = new MessageLog();


    /**
     * Logger for this classes.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Calculator.class);

    private static final JexlEngine JEXL = new JexlEngine(new SandBoxUberspect(),
                    new JexlArithmetic(true, MathContext.DECIMAL128, 2), null, Calculator.getMessageLog());
    static {
        JEXL.setDebug(true);
        JEXL.setSilent(false);
    }

    /**
     * @param _parameter
     * @param _rules
     */
    protected static void evaluate(final Parameter _parameter,
                                   final List<? extends AbstractRule<?>> _rules)
        throws EFapsException
    {
        Calculator.getMessageLog().clean();
        for (final AbstractRule<?> rule : _rules) {
            rule.prepare(_parameter);
        }
        final JexlContext context = new MapContext(AbstractParameter.getParameters(_parameter));
        context.set(Calculator.PARAKEY4CONTEXT, context);
        for (final AbstractRule<?> rule : _rules) {
            rule.evaluate(_parameter, context);
        }
    }

    protected static JexlEngine getJexlEngine()
    {
        return JEXL;
    }

    protected static MessageLog getMessageLog()
    {
        return MSGLOG;
    }

    protected static String getHtml4Rules(final Parameter _parameter,
                                          final List<? extends AbstractRule<?>> _rules)
        throws EFapsException
    {
        final Table table = new Table();
        for (final AbstractRule<?> rule : _rules) {
            table.addRow().addColumn(rule.getKey()).addColumn(rule.getDescription())
                            .addColumn(String.valueOf(rule.getResult())).addColumn(rule.getMessage());
        }
        return table.toHtml().toString();
    }

    protected static Result getResult(final Parameter _parameter,
                                      final List<? extends AbstractRule<?>> _rules)
        throws EFapsException
    {

        return new Result().addRules(_rules);
    }

    public static class Result
    {

        private final List<AbstractRule<?>> rules = new ArrayList<>();

        private final List<AbstractRule<?>> paymentRules = new ArrayList<>();

        private final List<AbstractRule<?>> deductionRules = new ArrayList<>();

        private final List<AbstractRule<?>> sumRules = new ArrayList<>();

        private final List<AbstractRule<?>> neutralRules = new ArrayList<>();

        private boolean initialized = false;

        private DecimalFormat formatter;

        protected void init()
        {
            if (!this.initialized) {
                for (final AbstractRule<?> rule : this.rules) {
                    switch (rule.getRuleType()) {
                        case PAYMENT:
                            this.paymentRules.add(rule);
                            break;
                        case DEDUCTION:
                            this.deductionRules.add(rule);
                            break;
                        case SUM:
                            this.sumRules.add(rule);
                            break;
                        case NEUTRAL:
                            this.neutralRules.add(rule);
                            break;
                        default:
                            break;
                    }
                }
                this.initialized = true;
            }
        }

        protected void reset()
        {
            if (this.initialized) {
                this.initialized = false;
                this.paymentRules.clear();
                this.deductionRules.clear();
                this.sumRules.clear();
                this.neutralRules.clear();
            }
        }

        public DecimalFormat getFormatter() throws EFapsException
        {

            return this.formatter == null ? NumberFormatter.get().getTwoDigitsFormatter() : this.formatter;
        }

        /**
         * @param _rules
         * @return
         */
        public Result addRules(final List<? extends AbstractRule<?>> _rules)
        {
            this.rules.addAll(_rules);

            return this;
        }

        /**
         * @return
         */
        public List<? extends AbstractRule<?>> getPaymentRules()
        {
            init();
            return this.paymentRules;
        }

        /**
         * @return
         */
        public List<? extends AbstractRule<?>> getDeductionRules()
        {
            init();
            return this.deductionRules;
        }

        /**
         * @return
         */
        public List<? extends AbstractRule<?>> getNeutralRules()
        {
            init();
            return this.neutralRules;
        }

        /**
         * @return
         */
        public List<? extends AbstractRule<?>> getSumRules()
        {
            init();
            return this.sumRules;
        }


        public BigDecimal getAmount(final List<? extends AbstractRule<?>> _rules)
        {
            BigDecimal ret = BigDecimal.ZERO;
            for (final AbstractRule<?> rule : _rules) {
                ret = ret.add(getBigDecimal(rule.getResult()));
            }
            return ret;
        }
        /**
         * @return
         */
        public BigDecimal getPayment()
        {
            return getAmount(getPaymentRules());
        }
        /**
         * @return
         */
        public String getPaymentFrmt() throws EFapsException
        {
            return getFormatter().format(getPayment());
        }
        /**
         * @return
         */
        public BigDecimal getDeduction()
        {
            return getAmount(getDeductionRules());
        }
        /**
         * @return
         */
        public String getDeductionFrmt() throws EFapsException
        {
            return getFormatter().format(getDeduction());
        }
        /**
         * @return
         */
        public BigDecimal getNeutral()
        {
            return getAmount(getNeutralRules());
        }

        /**
         * @return
         */
        public String getNeutralFrmt() throws EFapsException
        {
            return getFormatter().format(getNeutral());
        }
        /**
         * @return
         */
        public BigDecimal getSum()
        {
            return getAmount(getSumRules());
        }

        /**
         * @return
         */
        public String getSumFrmt() throws EFapsException
        {
            return getFormatter().format(getSum());
        }

        /**
         * @return
         */
        public BigDecimal getTotal()
        {
            return getPayment().subtract(getDeduction());
        }

        /**
         * @return
         */
        public String getTotalFrmt() throws EFapsException
        {
            return getFormatter().format(getTotal());
        }

        /**
         * @return
         */
        public BigDecimal getCost()
        {
            return getPayment().add(getNeutral());
        }

        /**
         * @return
         */
        public String getCostFrmt() throws EFapsException
        {
            return getFormatter().format(getCost());
        }


        public BigDecimal getBigDecimal(final Object _object)
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (_object != null) {
                if (_object instanceof BigDecimal) {
                    ret = (BigDecimal) _object;
                } else if (_object instanceof Double) {
                    ret = new BigDecimal((Double) _object);
                } else if (_object instanceof BigInteger) {
                    ret = new BigDecimal((BigInteger) _object);
                } else if (_object instanceof Long) {
                    ret = new BigDecimal((Long) _object);
                } else if (_object instanceof Integer) {
                    ret = new BigDecimal((Integer) _object);
                }
            }
            return ret;
        }


        /**
         * Setter method for instance variable {@link #formatter}.
         *
         * @param _formatter value for instance variable {@link #formatter}
         */
        public void setFormatter(final DecimalFormat _formatter)
        {
            this.formatter = _formatter;
        }


        /**
         * Getter method for the instance variable {@link #rules}.
         *
         * @return value of instance variable {@link #rules}
         */
        public List<AbstractRule<?>> getRules()
        {
            return this.rules;
        }
    }


    private static final class SandBoxUberspect
        extends UberspectImpl
    {

        private final Set<String> classNames = new HashSet<>();

        /**
         * @param _runtimeLogger
         * @param _theSandbox
         */
        public SandBoxUberspect()
        {
            super(Calculator.getMessageLog());
            try {
                final String whiteLstStr = Payroll.getSysConfig().getAttributeValue(
                                PayrollSettings.RULESANDBOXWHITELIST);
                if (whiteLstStr != null && !whiteLstStr.isEmpty()) {
                    for (final String white : whiteLstStr.split("\n")) {
                        this.classNames.add(white.trim());
                    }
                }
            } catch (final EFapsException e) {
                LOG.error("Error", e);
            }
        }

        @Override
        public JexlMethod getConstructorMethod(final Object ctorHandle,
                                               final Object[] args,
                                               final JexlInfo info)
        {
            final String className;
            if (ctorHandle instanceof Class<?>) {
                final Class<?> clazz = (Class<?>) ctorHandle;
                className = clazz.getName();
            } else if (ctorHandle != null) {
                className = ctorHandle.toString();
            } else {
                return null;
            }
            if (this.classNames.contains(className)) {
                return super.getConstructorMethod(className, args, info);
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JexlMethod getMethod(final Object obj,
                                    final String method,
                                    final Object[] args,
                                    final JexlInfo info)
        {
            if (obj != null && method != null) {
                if (this.classNames.contains(obj.getClass().getName())) {
                    return getMethodExecutor(obj, method, args);
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JexlPropertyGet getPropertyGet(final Object obj,
                                              final Object identifier,
                                              final JexlInfo info)
        {
            if (obj != null && identifier != null) {
                if (this.classNames.contains(obj.getClass().getName())) {
                    return super.getPropertyGet(obj, identifier.toString(), info);
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JexlPropertySet getPropertySet(final Object obj,
                                              final Object identifier,
                                              final Object arg,
                                              final JexlInfo info)
        {
            if (obj != null && identifier != null) {
                if (this.classNames.contains(obj.getClass().getName())) {
                    return super.getPropertySet(obj, identifier.toString(), arg, info);
                }
            }
            return null;
        }
    }

}
