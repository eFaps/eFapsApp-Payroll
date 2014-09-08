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

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
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
@EFapsUUID("e1c6f718-a232-47ed-beda-fa2cc87e985d")
@EFapsRevision("$Rev$")
public abstract class ExpressionRule_Base
    extends AbstractRule<ExpressionRule>
{

    @Override
    public void prepare(final Parameter _parameter)
        throws EFapsException
    {
        init();
    }

    @Override
    public void evaluate(final Parameter _parameter,
                         final JexlContext _context)
        throws EFapsException
    {
        final Expression expr = Calculator.getJexlEngine().createExpression(getExpression());
        final Object val = expr.evaluate(_context);
        _context.set(getKey4Expression(), val);
        setResult(val);
    }


    @Override
    protected ExpressionRule getThis()
    {
        return (ExpressionRule) this;
    }
}
