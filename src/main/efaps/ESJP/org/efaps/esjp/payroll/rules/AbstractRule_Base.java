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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.payroll.AbstractAlteration;
import org.efaps.esjp.payroll.util.Payroll.RuleConfig;
import org.efaps.esjp.payroll.util.Payroll.RuleType;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * 
 * @param <T> extension class
 */
@EFapsUUID("d31dd0ee-9396-4233-a4fe-bad8780cc931")
@EFapsApplication("eFapsApp-Payroll")
public abstract class AbstractRule_Base<T extends AbstractRule_Base<T>>
{


    /**
     * Instance of the rule.
     */
    private Instance instance;

    /**
     * Key of this rule.
     */
    private String key;

    private String expression;

    private String description;

    private String message;

    private boolean initialized = false;

    private Object result;

    private RuleType ruleType;

    private List<RuleConfig> config;

    private final Set<IRuleListener> ruleListeners = new HashSet<>();

    /**
     * Initialize the rule.
     *
     * @throws EFapsException on error
     */
    protected void init()
        throws EFapsException
    {
        if (!isInitialized()) {
            setInitialized(true);
            final PrintQuery print = new PrintQuery(getInstance());
            print.addAttribute(CIPayroll.RuleAbstract.Key, CIPayroll.RuleAbstract.Description,
                            CIPayroll.RuleAbstract.Expression, CIPayroll.RuleAbstract.RuleType,
                            CIPayroll.RuleAbstract.Config);
            print.execute();
            initInternal(print);
            setKey(print.<String>getAttribute(CIPayroll.RuleAbstract.Key));
            setDescription(print.<String>getAttribute(CIPayroll.RuleAbstract.Description));
            setRuleType(print.<RuleType>getAttribute(CIPayroll.RuleAbstract.RuleType));
            setConfig(print.<List<RuleConfig>>getAttribute(CIPayroll.RuleAbstract.Config));

            if (getConfig() != null && getConfig().contains(RuleConfig.EVALUATEALTERATION)) {
                addRuleListener(AbstractAlteration.getAlterationListener(this));
            }
        }
    }

    protected void initInternal(final PrintQuery _print)
        throws EFapsException
    {
        setExpression(_print.<String>getAttribute(CIPayroll.RuleAbstract.Expression));
    }

    protected abstract T getThis();

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @throws EFapsException on error
     */
    public abstract void prepare(final Parameter _parameter)
        throws EFapsException;

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _context JexlContext
     * @throws EFapsException on error
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
     * @return this for chaining
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
     * @throws EFapsException on error
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
     * @throws EFapsException on error
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
     * @return this for chaining
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
     * @throws EFapsException on error
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
     * @return this for chaining
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
     * @throws EFapsException on error
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
     * @return this for chaining
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
     * @return this for chaining
     * @throws EFapsException on error
     *
     */
    public T setResult(final Object _result)
        throws EFapsException
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

    /**
     * Getter method for the instance variable {@link #ruletype}.
     *
     * @return value of instance variable {@link #ruletype}
     */
    public RuleType getRuleType()
    {
        return this.ruleType;
    }

    /**
     * Setter method for instance variable {@link #ruletype}.
     *
     * @param _ruletype value for instance variable {@link #ruletype}
     */
    public void setRuleType(final RuleType _ruletype)
    {
        this.ruleType = _ruletype;
    }

    /**
     * @return true if the rule should be added as position else false
     */
    public boolean add()
    {
        boolean ret = true;
        final Object obj = getResult();
        if (getConfig() != null && getConfig().contains(RuleConfig.EXCLUDEZERO)) {
            if (obj != null) {
                if (obj instanceof BigDecimal) {
                    ret = ((BigDecimal) obj).compareTo(BigDecimal.ZERO) > 0;
                } else if (obj instanceof Integer) {
                    ret = (Integer) obj > 0;
                } else if (obj instanceof Double) {
                    ret = (Double) obj > 0;
                } else if (obj instanceof Float) {
                    ret = (Float) obj > 0;
                }
            }
        }
        return ret;
    }

    /**
     * Getter method for the instance variable {@link #config}.
     *
     * @return value of instance variable {@link #config}
     */
    public List<RuleConfig> getConfig()
    {
        return this.config;
    }

    /**
     * Setter method for instance variable {@link #config}.
     *
     * @param _config value for instance variable {@link #config}
     */
    public void setConfig(final List<RuleConfig> _config)
    {
        this.config = _config;
    }

    /**
     * Getter method for the instance variable {@link #ruleListeners}.
     *
     * @return value of instance variable {@link #ruleListeners}
     */
    public Set<IRuleListener> getRuleListeners()
    {
        return this.ruleListeners;
    }

    /**
     * @param _clazz class of listsner wanted
     * @param <S> Class type
     * @return RuleListener
     */
    public <S extends IRuleListener> Set<S> getRuleListeners(final Class<S> _clazz)
    {
        final Set<S> ret = new HashSet<>();
        for (final Object listener : getRuleListeners()) {
            if (_clazz.isInstance(listener)) {
                ret.add(_clazz.cast(listener));
            }
        }
        return ret;
    }

      /**
     * @param _ruleListener rulelistener to be added
     * @return this for chaining
     */
    public T addRuleListener(final IRuleListener _ruleListener)
    {
        getRuleListeners().add(_ruleListener);
        return getThis();
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

    /**
     * @param _ruleInsts instance of rules the rule object is wanetd for
     * @return lsit of rul objects
     */
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
}
