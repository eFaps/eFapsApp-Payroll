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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("796fbef5-22f2-42a2-99f8-9d0b0699d6f7")
@EFapsRevision("$Rev$")
public abstract class Result_Base
{

    private final List<AbstractRule<?>> rules = new ArrayList<>();

    private final List<AbstractRule<?>> paymentRules = new ArrayList<>();

    private final List<AbstractRule<?>> deductionRules = new ArrayList<>();

    private final List<AbstractRule<?>> sumRules = new ArrayList<>();

    private final List<AbstractRule<?>> neutralRules = new ArrayList<>();

    private boolean initialized = false;

    private DecimalFormat formatter;

    protected void init()
    {
        if (!this.initialized) {
            for (final AbstractRule<?> rule : this.rules) {
                switch (rule.getRuleType()) {
                    case PAYMENT:
                        this.paymentRules.add(rule);
                        break;
                    case DEDUCTION:
                        this.deductionRules.add(rule);
                        break;
                    case SUM:
                        this.sumRules.add(rule);
                        break;
                    case NEUTRAL:
                        this.neutralRules.add(rule);
                        break;
                    default:
                        break;
                }
            }
            this.initialized = true;
        }
    }

    protected void reset()
    {
        if (this.initialized) {
            this.initialized = false;
            this.paymentRules.clear();
            this.deductionRules.clear();
            this.sumRules.clear();
            this.neutralRules.clear();
        }
    }

    public DecimalFormat getFormatter() throws EFapsException
    {

        return this.formatter == null ? NumberFormatter.get().getTwoDigitsFormatter() : this.formatter;
    }

    /**
     * @param _rules
     * @return
     */
    public Result addRules(final List<? extends AbstractRule<?>> _rules)
    {
        this.rules.addAll(_rules);
        return (Result) this;
    }

    /**
     * @return
     */
    public List<? extends AbstractRule<?>> getPaymentRules()
    {
        init();
        return this.paymentRules;
    }

    /**
     * @return
     */
    public List<? extends AbstractRule<?>> getDeductionRules()
    {
        init();
        return this.deductionRules;
    }

    /**
     * @return
     */
    public List<? extends AbstractRule<?>> getNeutralRules()
    {
        init();
        return this.neutralRules;
    }

    /**
     * @return
     */
    public List<? extends AbstractRule<?>> getSumRules()
    {
        init();
        return this.sumRules;
    }


    public BigDecimal getAmount(final List<? extends AbstractRule<?>> _rules)
    {
        BigDecimal ret = BigDecimal.ZERO;
        for (final AbstractRule<?> rule : _rules) {
            ret = ret.add(getBigDecimal(rule.getResult()));
        }
        return ret;
    }
    /**
     * @return
     */
    public BigDecimal getPayment()
    {
        return getAmount(getPaymentRules());
    }
    /**
     * @return
     */
    public String getPaymentFrmt() throws EFapsException
    {
        return getFormatter().format(getPayment());
    }
    /**
     * @return
     */
    public BigDecimal getDeduction()
    {
        return getAmount(getDeductionRules());
    }
    /**
     * @return
     */
    public String getDeductionFrmt() throws EFapsException
    {
        return getFormatter().format(getDeduction());
    }
    /**
     * @return
     */
    public BigDecimal getNeutral()
    {
        return getAmount(getNeutralRules());
    }

    /**
     * @return
     */
    public String getNeutralFrmt() throws EFapsException
    {
        return getFormatter().format(getNeutral());
    }
    /**
     * @return
     */
    public BigDecimal getSum()
    {
        return getAmount(getSumRules());
    }

    /**
     * @return
     */
    public String getSumFrmt() throws EFapsException
    {
        return getFormatter().format(getSum());
    }

    /**
     * @return
     */
    public BigDecimal getTotal()
    {
        return getPayment().subtract(getDeduction());
    }

    /**
     * @return
     */
    public String getTotalFrmt() throws EFapsException
    {
        return getFormatter().format(getTotal());
    }

    /**
     * @return
     */
    public BigDecimal getCost()
    {
        return getPayment().add(getNeutral());
    }

    /**
     * @return
     */
    public String getCostFrmt() throws EFapsException
    {
        return getFormatter().format(getCost());
    }

    public BigDecimal getBigDecimal(final Object _object)
    {
        return Calculator_Base.getBigDecimal(_object);
    }

    /**
     * Setter method for instance variable {@link #formatter}.
     *
     * @param _formatter value for instance variable {@link #formatter}
     */
    public void setFormatter(final DecimalFormat _formatter)
    {
        this.formatter = _formatter;
    }


    /**
     * Getter method for the instance variable {@link #rules}.
     *
     * @return value of instance variable {@link #rules}
     */
    public List<AbstractRule<?>> getRules()
    {
        return this.rules;
    }
}
