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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: AbstractRule_Base.java 13971 2014-09-08 21:03:58Z
 *          jan@moxter.net $
 */
@EFapsUUID("d31dd0ee-9396-4233-a4fe-bad8780cc931")
@EFapsRevision("$Rev$")
public abstract class AbstractRule_Base<T>
{
    /**
     * Instance of the rule.
     */
    private Instance instance;

    private String key;

    private String expression;

    private String description;

    private String message;

    private boolean initialized = false;

    private Object result;

    protected void init()
        throws EFapsException
    {
        if (!isInitialized()) {
            setInitialized(true);
            final PrintQuery print = new PrintQuery(getInstance());
            print.addAttribute(CIPayroll.RuleAbstract.Key, CIPayroll.RuleAbstract.Description,
                            CIPayroll.RuleAbstract.Expression);
            print.execute();
            initInternal(print);
            setKey(print.<String>getAttribute(CIPayroll.RuleAbstract.Key));
            setDescription(print.<String>getAttribute(CIPayroll.RuleAbstract.Description));
        }
    }

    protected void initInternal(final PrintQuery _print)
        throws EFapsException
    {
        setExpression(_print.<String>getAttribute(CIPayroll.RuleAbstract.Expression));
    }

    protected abstract T getThis();

    /**
     * @param _parameter
     */
    public abstract void prepare(final Parameter _parameter)
        throws EFapsException;

    /**
     * @param _parameter
     * @param _context
     */
    public abstract void evaluate(final Parameter _parameter,
                                  final JexlContext _context)
        throws EFapsException;

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

    /**
     * Getter method for the instance variable {@link #initialized}.
     *
     * @return value of instance variable {@link #initialized}
     */
    public boolean isInitialized()
    {
        return this.initialized;
    }

    /**
     * Setter method for instance variable {@link #initialized}.
     *
     * @param _initialized value for instance variable {@link #initialized}
     */
    public void setInitialized(final boolean _initialized)
    {
        this.initialized = _initialized;
    }

    /**
     * Getter method for the instance variable {@link #key}.
     *
     * @return value of instance variable {@link #key}
     */
    public String getKey()
        throws EFapsException
    {
        init();
        return this.key;
    }

    /**
     * Getter method for the instance variable {@link #key}.
     *
     * @return value of instance variable {@link #key}
     */
    public String getKey4Expression()
        throws EFapsException
    {
        String ret;
        if (Character.isDigit(getKey().charAt(0))) {
            ret = "$" + getKey();
        } else {
            ret = getKey();
        }
        return ret;
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
     * Getter method for the instance variable {@link #expression}.
     *
     * @return value of instance variable {@link #expression}
     */
    public String getExpression()
        throws EFapsException
    {
        init();
        return this.expression;
    }

    /**
     * Setter method for instance variable {@link #expression}.
     *
     * @param _expression value for instance variable {@link #expression}
     */
    public T setExpression(final String _expression)
    {
        this.expression = _expression;
        return getThis();
    }

    /**
     * Getter method for the instance variable {@link #description}.
     *
     * @return value of instance variable {@link #description}
     */
    public String getDescription()
        throws EFapsException
    {
        init();
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
     * Getter method for the instance variable {@link #result}.
     *
     * @return value of instance variable {@link #result}
     */
    public Object getResult()
    {
        return this.result;
    }

    /**
     * Setter method for instance variable {@link #result}.
     *
     * @param _result value for instance variable {@link #result}
     */
    public T setResult(final Object _result)
    {
        this.result = _result;
        return getThis();
    }

    /**
     * Getter method for the instance variable {@link #message}.
     *
     * @return value of instance variable {@link #message}
     */
    public String getMessage()
    {
        return this.message;
    }

    /**
     * Setter method for instance variable {@link #message}.
     *
     * @param _message value for instance variable {@link #message}
     */
    public void setMessage(final String _message)
    {
        this.message = _message;
    }

    protected static List<? extends AbstractRule<?>> getRules(final Instance... _ruleInsts)
    {
        final List<AbstractRule<?>> ret = new ArrayList<>();
        for (final Instance inst : _ruleInsts) {
            if (inst.getType().isCIType(CIPayroll.RuleInput)) {
                ret.add(new InputRule().setInstance(inst));
            } else if (inst.getType().isCIType(CIPayroll.RuleExpression)) {
                ret.add(new ExpressionRule().setInstance(inst));
            }
        }
        return ret;
    }

    @Override
    public String toString()
    {
        String keyTmp = null;
        try {
            keyTmp = getKey();
        } catch (final EFapsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new ToStringBuilder(this)
                        .append("key", keyTmp)
                        .append("result", getResult()).toString();
    }
}
