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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    private static final DecimalFormat JEXLFORMATER = (DecimalFormat) DecimalFormat.getInstance();
    static {
        JEXLFORMATER.setGroupingUsed(false);
        JEXLFORMATER.setMinimumFractionDigits(2);
        JEXLFORMATER.setMaximumFractionDigits(2);
    }
    /**
     * Logger for this classes.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Calculator.class);

    private static final JexlEngine JEXL = new JexlEngine(new SandBoxUberspect(),
                    new JexlArithmetic(true, MathContext.DECIMAL128, 2), null, Calculator.getMessageLog());
    static {
        JEXL.setDebug(true);
        JEXL.setSilent(false);

        final Map<String, Object> functions = new HashMap<>();
        functions.put("math", MathFunctions.class);
        functions.put("data", DataFunctions.class);
        JEXL.setFunctions(functions);
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

    protected static String toJexlBigDecimal(final Parameter _parameter,
                                     final BigDecimal _bigDecimal)
        throws EFapsException
    {
        return JEXLFORMATER.format(_bigDecimal)  + "B";
    }

    protected static BigDecimal getBigDecimal(final Object _object)
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
                this.classNames.add(MathFunctions.class.getName());
                this.classNames.add(DataFunctions.class.getName());
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
