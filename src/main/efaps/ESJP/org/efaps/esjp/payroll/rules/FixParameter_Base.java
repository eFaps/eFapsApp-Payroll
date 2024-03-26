/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.payroll.rules;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.MapContext;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: FixParameter_Base.java 13971 2014-09-08 21:03:58Z
 *          jan@moxter.net $
 */
@EFapsUUID("6f768e9c-b423-46f9-8e32-cfcdc774dee5")
@EFapsApplication("eFapsApp-Payroll")
public abstract class FixParameter_Base
    extends AbstractParameter<FixParameter>
{

    @Override
    protected FixParameter getThis()
    {
        return (FixParameter) this;
    }

    @Override
    public Object getValue()
        throws EFapsException
    {
        final Expression expr = Calculator.getJexlEngine().createExpression(super.getValue().toString());
        return expr.evaluate(new MapContext());
    }
}
