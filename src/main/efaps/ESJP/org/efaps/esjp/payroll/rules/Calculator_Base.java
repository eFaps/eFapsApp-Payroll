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

import java.math.MathContext;
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
import org.apache.commons.logging.LogFactory;
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

    /**
     * Logger for this classes.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Calculator.class);

    private static final JexlEngine JEXL = new JexlEngine(new SandBoxUberspect(),
                    new JexlArithmetic(true, MathContext.DECIMAL128, 2), null, null);
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

    protected static String getHtml4Rules(final Parameter _parameter,
                                          final List<? extends AbstractRule<?>> _rules)
        throws EFapsException
    {
        final Table table = new Table();
        for (final AbstractRule<?> rule : _rules) {
            table.addRow().addColumn(rule.getKey()).addColumn(rule.getDescription())
                            .addColumn(String.valueOf(rule.getResult()));
        }
        return table.toHtml().toString();
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
            super(LogFactory.getLog(JexlEngine.class));
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
