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

import org.apache.commons.jexl2.JexlContext;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * 
 */
@EFapsUUID("d58cdd40-4039-4e59-b4b0-0514508a61dc")
@EFapsApplication("eFapsApp-Payroll")
public abstract class MathFunctions_Base
{

    /**
     * JexlContext calling the instance.
     */
    private final JexlContext context;

    /**
     * @param _context JexlContext calling the instance.
     */
    public MathFunctions_Base(final JexlContext _context)
    {
        this.context = _context;
    }

    /**
     * Getter method for the instance variable {@link #context}.
     *
     * @return value of instance variable {@link #context}
     */
    public JexlContext getContext()
    {
        return this.context;
    }

    /**
     * @param _object objt to round
     * @param _scale sacle to round to
     * @return rounded bigdecimal
     */
    public Object round(final Object _object,
                        final int _scale)
    {
        final BigDecimal number = Calculator.getBigDecimal(_object);
        return number.setScale(_scale, BigDecimal.ROUND_HALF_UP);
    }
}
