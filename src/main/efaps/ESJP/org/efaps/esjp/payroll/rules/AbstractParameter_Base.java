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

import java.util.HashMap;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("45fcf09d-d676-4ac1-8f15-5f790f2eaf03")
@EFapsRevision("$Rev$")
public abstract class AbstractParameter_Base<T>
{
    /**
     * Instance of the rule.
     */
    private Instance instance;

    private String key;

    private Object value;

    private String description;

    protected abstract T getThis();

    /**
     * Getter method for the instance variable {@link #key}.
     *
     * @return value of instance variable {@link #key}
     */
    public String getKey()
    {
        return this.key;
    }

    /**
     * Setter method for instance variable {@link #key}.
     *
     * @param _key value for instance variable {@link #key}
     */
    public T setKey(final String _key)
    {
        this.key = _key;
        return getThis();
    }

    /**
     * Getter method for the instance variable {@link #value}.
     *
     * @return value of instance variable {@link #value}
     */
    public Object getValue()
    {
        return this.value;
    }

    /**
     * Setter method for instance variable {@link #value}.
     *
     * @param _value value for instance variable {@link #value}
     */
    public T setValue(final Object _value)
    {
        this.value = _value;
        return getThis();
    }

    /**
     * Getter method for the instance variable {@link #description}.
     *
     * @return value of instance variable {@link #description}
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * Setter method for instance variable {@link #description}.
     *
     * @param _description value for instance variable {@link #description}
     */
    public T setDescription(final String _description)
    {
        this.description = _description;
        return getThis();
    }

    /**
     * Getter method for the instance variable {@link #instance}.
     *
     * @return value of instance variable {@link #instance}
     */
    public Instance getInstance()
    {
        return this.instance;
    }

    /**
     * Setter method for instance variable {@link #instance}.
     *
     * @param _instance value for instance variable {@link #instance}
     */
    public T setInstance(final Instance _instance)
    {
        this.instance = _instance;
        return getThis();
    }


    protected static Map<String, Object> getParameters(final Parameter _parameter)
        throws EFapsException
    {
        final Map<String, Object> ret = new HashMap<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.ParameterAbstract);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.ParameterAbstract.Key, CIPayroll.ParameterAbstract.Value);
        multi.execute();
        while (multi.next()) {
            if (multi.getCurrentInstance().getType().isCIType(CIPayroll.ParameterFix)) {
               final FixParameter para = new FixParameter()
                    .setInstance(multi.getCurrentInstance())
                    .setKey(multi.<String>getAttribute(CIPayroll.ParameterAbstract.Key))
                    .setValue(multi.<String>getAttribute(CIPayroll.ParameterAbstract.Value));
               ret.put(para.getKey(), para.getValue());
            }
        }
        return ret;
    }
}
