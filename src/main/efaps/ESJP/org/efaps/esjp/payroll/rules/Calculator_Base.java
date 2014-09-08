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

import java.util.List;

import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("a18eb023-438e-442b-853e-3b5b61517ca8")
@EFapsRevision("$Rev$")
public abstract class Calculator_Base
{

    private static final JexlEngine JEXL = new JexlEngine();
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
        for (final AbstractRule<?> rule : _rules) {
            rule.evaluate(_parameter, context);
        }
    }

    protected static JexlEngine getJexlEngine()
    {
        return JEXL;
    }
}
