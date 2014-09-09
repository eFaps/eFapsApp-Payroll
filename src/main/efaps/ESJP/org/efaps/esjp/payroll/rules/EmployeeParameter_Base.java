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

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: FixParameter_Base.java 13971 2014-09-08 21:03:58Z
 *          jan@moxter.net $
 */
@EFapsUUID("b9d07114-c113-4992-becc-95e319181259")
@EFapsRevision("$Rev$")
public abstract class EmployeeParameter_Base
    extends AbstractParameter<EmployeeParameter>
{

    private Instance employeeInstance;

    private String select;

    @Override
    protected EmployeeParameter getThis()
    {
        return (EmployeeParameter) this;
    }

    @Override
    public Object getValue()
        throws EFapsException
    {
        if (super.getValue() == null) {
            final PrintQuery print = new PrintQuery(getEmployeeInstance());
            print.addSelect(getSelect());
            print.executeWithoutAccessCheck();
            super.setValue(print.getSelect(getSelect()));
        }
        return super.getValue();
    }

    /**
     * Getter method for the instance variable {@link #employeeInstance}.
     *
     * @return value of instance variable {@link #employeeInstance}
     */
    public Instance getEmployeeInstance()
    {
        return this.employeeInstance;
    }

    /**
     * Setter method for instance variable {@link #employeeInstance}.
     *
     * @param _employeeInstance value for instance variable
     *            {@link #employeeInstance}
     */
    public EmployeeParameter setEmployeeInstance(final Instance _employeeInstance)
    {
        this.employeeInstance = _employeeInstance;
        return getThis();
    }

    /**
     * Getter method for the instance variable {@link #select}.
     *
     * @return value of instance variable {@link #select}
     */
    public String getSelect()
    {
        return this.select;
    }

    /**
     * Setter method for instance variable {@link #select}.
     *
     * @param _select value for instance variable {@link #select}
     */
    public EmployeeParameter setSelect(final String _select)
    {
        this.select = _select;
        return getThis();
    }

}
