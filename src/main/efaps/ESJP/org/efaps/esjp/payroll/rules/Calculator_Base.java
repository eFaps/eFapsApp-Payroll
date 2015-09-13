/*
 * Copyright 2003 - 2015 The eFaps Team
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
import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsClassLoader;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.payroll.util.Payroll;
import org.efaps.esjp.ui.html.Table;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("a18eb023-438e-442b-853e-3b5b61517ca8")
@EFapsApplication("eFapsApp-Payroll")
public abstract class Calculator_Base
{

    /**
     * Key to get the jexlcontext inside a rule.
     */
    protected static final String PARAKEY4CONTEXT = "Context";

    /**
     * Key for a parameter containing the current rule.
     */
    protected static final String PARAKEY4CURRENTRULE = "CurrentRule";

    /**
     * Logging instance.
     */
    private static MessageLog MSGLOG = new MessageLog();

    /**
     * Formatter instance for jexl.
     */
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

    /**
     * The engine of jexl.
     */
    private static final JexlEngine JEXL = new JexlEngine(new SandBoxUberspect(),
                    new JexlArithmetic(true, MathContext.DECIMAL128, 2), null, Calculator.getMessageLog());
    static {
        JEXL.setDebug(true);
        JEXL.setSilent(false);

        final Map<String, Object> functions = new HashMap<>();
        functions.put("math", MathFunctions.class);
        functions.put("data", DataFunctions.class);
        functions.put("log", MSGLOG);
        JEXL.setFunctions(functions);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _rules List of rules
     * @throws EFapsException on error
     */
    protected static void evaluate(final Parameter _parameter,
                                   final List<? extends AbstractRule<?>> _rules)
        throws EFapsException
    {
        evaluate(_parameter, _rules, null);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _rules List of rules
     * @param _docInst instance of the actual document
     * @throws EFapsException on error
     */
    protected static void evaluate(final Parameter _parameter,
                                   final List<? extends AbstractRule<?>> _rules,
                                   final Instance _docInst)
        throws EFapsException
    {
        Calculator.getMessageLog().clean();
        for (final AbstractRule<?> rule : _rules) {
            rule.prepare(_parameter);
        }
        final JexlContext context = new MapContext(AbstractParameter.getParameters(_parameter, _docInst));
        context.set(Calculator.PARAKEY4CONTEXT, context);
        for (final AbstractRule<?> rule : _rules) {
            context.set(Calculator.PARAKEY4CURRENTRULE, rule);
            rule.evaluate(_parameter, context);
        }
    }

    /**
     * @return the instance of the jexl engine
     */
    protected static JexlEngine getJexlEngine()
    {
        return JEXL;
    }

    /**
     * @return the instance of the logger
     */
    protected static MessageLog getMessageLog()
    {
        return MSGLOG;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _rules List of rules the html is wanted for
     * @return html table
     * @throws EFapsException on error
     */
    protected static String getHtml4Rules(final Parameter _parameter,
                                          final List<? extends AbstractRule<?>> _rules)
        throws EFapsException
    {
        final Table table = new Table();
        for (final AbstractRule<?> rule : _rules) {
            table.addRow().addColumn(StringEscapeUtils.escapeHtml4(rule.getKey()))
                            .addColumn(StringEscapeUtils.escapeHtml4(rule.getDescription()))
                            .addColumn(StringEscapeUtils.escapeHtml4(String.valueOf(rule.getResult())))
                            .addColumn(StringEscapeUtils.escapeHtml4(rule.getMessage()).replace("\n", "<br/>"));
        }
        return table.toHtml().toString();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _rules List of rules
     * @return the result
     * @throws EFapsException on error
     */
    protected static Result getResult(final Parameter _parameter,
                                      final List<? extends AbstractRule<?>> _rules)
        throws EFapsException
    {
        return new Result().addRules(_rules);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _bigDecimal BigDecimal to be converted
     * @return string for BigDecimal
     * @throws EFapsException on error
     */
    protected static String toJexlBigDecimal(final Parameter _parameter,
                                             final BigDecimal _bigDecimal)
        throws EFapsException
    {
        return JEXLFORMATER.format(_bigDecimal) + "B";
    }

    /**
     * @param _object get an Bigedecimal
     * @return BigDecimal
     */
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
            } else if (_object instanceof Float) {
                ret = new BigDecimal((Float) _object);
            }
        }
        return ret;
    }

    /**
     * sandbox mechanism.
     */
    private static final class SandBoxUberspect
        extends UberspectImpl
    {

        /**
         * Set of allowed classnames.
         */
        private final Set<String> classNames = new HashSet<>();

        /**
         * @param _runtimeLogger
         * @param _theSandbox
         */
        public SandBoxUberspect()
        {
            super(Calculator.getMessageLog());
            setClassLoader(EFapsClassLoader.getInstance());
            setLoader(EFapsClassLoader.getInstance());
            try {
                for (final String white : Payroll.RULESANDBOXWHITELIST.get()) {
                    this.classNames.add(white.trim());
                }
                this.classNames.add(MathFunctions.class.getName());
                this.classNames.add(DataFunctions.class.getName());
                this.classNames.add(MessageLog.class.getName());
            } catch (final EFapsException e) {
                LOG.error("Error", e);
            }
        }

        @Override
        public JexlMethod getConstructorMethod(final Object _ctorHandle,
                                               final Object[] _args,
                                               final JexlInfo _info)
        {
            setClassLoader(EFapsClassLoader.getInstance());
            JexlMethod ret = null;
            final String className;
            if (_ctorHandle instanceof Class<?>) {
                final Class<?> clazz = (Class<?>) _ctorHandle;
                className = clazz.getName();
            } else if (_ctorHandle != null) {
                className = _ctorHandle.toString();
            } else {
                className = "NOTFOUND";
            }
            if (this.classNames.contains(className)) {
                ret = super.getConstructorMethod(className, _args, _info);
            }
            return ret;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JexlMethod getMethod(final Object _obj,
                                    final String _method,
                                    final Object[] _args,
                                    final JexlInfo _info)
        {
            JexlMethod ret = null;
            if (_obj != null && _method != null) {
                if (this.classNames.contains(_obj.getClass().getName())
                                || _obj instanceof Class && this.classNames.contains(((Class<?>) _obj).getName())) {
                    ret = getMethodExecutor(_obj, _method, _args);
                }
            }
            return ret;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JexlPropertyGet getPropertyGet(final Object _obj,
                                              final Object _identifier,
                                              final JexlInfo _info)
        {
            JexlPropertyGet ret = null;
            if (_obj != null && _identifier != null) {
                if (this.classNames.contains(_obj.getClass().getName())) {
                    ret = super.getPropertyGet(_obj, _identifier.toString(), _info);
                }
            }
            return ret;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JexlPropertySet getPropertySet(final Object _obj,
                                              final Object _identifier,
                                              final Object _arg,
                                              final JexlInfo _info)
        {
            JexlPropertySet ret = null;
            if (_obj != null && _identifier != null) {
                if (this.classNames.contains(_obj.getClass().getName())) {
                    ret = super.getPropertySet(_obj, _identifier.toString(), _arg, _info);
                }
            }
            return ret;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<?> getClassByName(final String _className)
        {
            Class<?> ret = null;
            try {
                ret = EFapsClassLoader.getInstance().loadClass(_className);
            } catch (final ClassNotFoundException e) {
                LOG.debug("ClassNotFoundException", e);
            }
            return ret;
        }
    }
}
