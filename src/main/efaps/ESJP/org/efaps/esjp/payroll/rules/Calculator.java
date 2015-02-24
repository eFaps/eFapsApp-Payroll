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
import java.util.List;

import org.apache.commons.jexl2.JexlEngine;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_Base</code>"
 * class.
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("19b31ad6-83d0-4c42-86b0-70866573b39d")
@EFapsRevision("$Rev$")
public class Calculator
    extends Calculator_Base
{

    public static final String PARAKEY4CONTEXT = Calculator_Base.PARAKEY4CONTEXT;

    /**
     * Key for a parameter containing the current rule.
     */
    public static final String PARAKEY4CURRENTRULE = Calculator_Base.PARAKEY4CURRENTRULE;

    /**
     * @param _parameter
     * @param _rules
     */
    public static void evaluate(final Parameter _parameter,
                                final List<? extends AbstractRule<?>> _rules)
        throws EFapsException
    {
        Calculator_Base.evaluate(_parameter, _rules);
    }

    public static void evaluate(final Parameter _parameter,
                                final List<? extends AbstractRule<?>> _rules,
                                final Instance _docInst)
        throws EFapsException
    {
        Calculator_Base.evaluate(_parameter, _rules, _docInst);
    }

    public static JexlEngine getJexlEngine()
    {
        return Calculator_Base.getJexlEngine();
    }

    public static String getHtml4Rules(final Parameter _parameter,
                                       final List<? extends AbstractRule<?>> _rules)
        throws EFapsException
    {
        return Calculator_Base.getHtml4Rules(_parameter, _rules);
    }

    public static MessageLog getMessageLog()
    {
        return Calculator_Base.getMessageLog();
    }

    public static Result getResult(final Parameter _parameter,
                                   final List<? extends AbstractRule<?>> _rules)
        throws EFapsException
    {
        return Calculator_Base.getResult(_parameter, _rules);
    }

    public static String toJexlBigDecimal(final Parameter _parameter,
                                          final BigDecimal _bigDecimal)
        throws EFapsException
    {
        return Calculator_Base.toJexlBigDecimal(_parameter, _bigDecimal);
    }

    public static BigDecimal getBigDecimal(final Object _object)
    {
        return Calculator_Base.getBigDecimal(_object);
    }

}
