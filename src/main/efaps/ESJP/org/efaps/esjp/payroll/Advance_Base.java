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

package org.efaps.esjp.payroll;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.ci.CITablePayroll;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.payroll.rules.AbstractRule;
import org.efaps.esjp.payroll.rules.Calculator;
import org.efaps.esjp.payroll.rules.Result;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.document.AbstractDocument;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("973456d7-c147-4f3b-893e-0f7852d4d084")
@EFapsRevision("$Rev$")
public abstract class Advance_Base
    extends AbstractDocument
{

    /**
     * Logger for this classes.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Advance.class);

    /**
     * Create advances.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue(CIFormPayroll.Payroll_AdvanceForm.date.name);

        final String[] employees = _parameter.getParameterValues(CITablePayroll.Payroll_AdvanceTable.employee.name);
        final String[] amount2Pays = _parameter
                        .getParameterValues(CITablePayroll.Payroll_AdvanceTable.rateCrossTotal.name);
        final String[] currencyLinks = _parameter
                        .getParameterValues(CITablePayroll.Payroll_AdvanceTable.rateCurrencyId.name);
        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
        final Dimension timeDim = CIPayroll.Payslip.getType().getAttribute(CIPayroll.Payslip.LaborTime.name)
                        .getDimension();
        try {
            final Instance baseCurIns = Currency.getBaseCurrency();
            final PriceUtil util = new PriceUtil();
            for (int i = 0; i < employees.length; i++) {
                if (employees[i] != null && !employees[i].isEmpty()) {
                    final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(), currencyLinks[i]);

                    final BigDecimal[] rates = util
                                    .getExchangeRate(util.getDateFromParameter(_parameter), rateCurrInst);
                    final CurrencyInst cur = new CurrencyInst(rateCurrInst);
                    final Object[] rate = new Object[] { cur.isInvert() ? BigDecimal.ONE : rates[1],
                                    cur.isInvert() ? rates[1] : BigDecimal.ONE };

                    final BigDecimal ratePay = amount2Pays[i] != null && !amount2Pays[i].isEmpty()
                                    ? (BigDecimal) formater.parse(amount2Pays[i])
                                    : BigDecimal.ZERO;

                    final BigDecimal pay = ratePay.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : ratePay
                                    .setScale(8, BigDecimal.ROUND_HALF_UP).divide(rates[0], BigDecimal.ROUND_HALF_UP)
                                    .setScale(2, BigDecimal.ROUND_HALF_UP);
                    final Insert insert = new Insert(CIPayroll.Advance);

                    final String name = getDocName4Create(_parameter);
                    insert.add(CIPayroll.Advance.Name, name);
                    insert.add(CIPayroll.Advance.Date, date);
                    insert.add(CIPayroll.Advance.EmployeeAbstractLink, Instance.get(employees[i]));
                    insert.add(CIPayroll.Advance.Status, Status.find(CIPayroll.AdvanceStatus.Draft));
                    insert.add(CIPayroll.Advance.RateCrossTotal, ratePay);
                    insert.add(CIPayroll.Advance.RateNetTotal, ratePay);
                    insert.add(CIPayroll.Advance.CrossTotal, pay);
                    insert.add(CIPayroll.Advance.NetTotal, pay);
                    insert.add(CIPayroll.Advance.RateCurrencyId, rateCurrInst);
                    insert.add(CIPayroll.Advance.CurrencyId, baseCurIns);
                    insert.add(CIPayroll.Advance.AmountCost, pay);
                    insert.add(CIPayroll.Advance.Rate, rate);
                    insert.add(CIPayroll.Advance.DiscountTotal, 0);
                    insert.add(CIPayroll.Advance.RateDiscountTotal, 0);
                    insert.add(CIPayroll.Advance.LaborTime, new Object[] { 0, timeDim.getBaseUoM().getId() });
                    insert.add(CIPayroll.Advance.ExtraLaborTime, new Object[] { 0, timeDim.getBaseUoM().getId() });
                    insert.add(CIPayroll.Advance.NightLaborTime, new Object[] { 0, timeDim.getBaseUoM().getId() });
                    insert.add(CIPayroll.Advance.HolidayLaborTime, new Object[] { 0, timeDim.getBaseUoM().getId() });
                    insert.execute();
                }
            }
        } catch (final ParseException e) {
            throw new EFapsException(Payslip_Base.class, "createMassive.ParseException", e);
        }
        return new Return();
    }

    public Return createMultiple(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> templates = getInstances(_parameter,
                        CIFormPayroll.Payroll_AdvanceCreateMultipleForm.templates.name);
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIHumanResource.EmployeeAbstract);
        attrQueryBldr.addWhereAttrEqValue(CIHumanResource.EmployeeAbstract.StatusAbstract,
                        Status.find(CIHumanResource.EmployeeStatus.Worker));

        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Template2EmployeeAbstract);
        queryBldr.addWhereAttrInQuery(CIPayroll.Template2EmployeeAbstract.ToAbstractLink,
                        attrQueryBldr.getAttributeQuery(CIHumanResource.EmployeeAbstract.ID));
        queryBldr.addWhereAttrEqValue(CIPayroll.Template2EmployeeAbstract.FromAbstractLink, templates.toArray());

        final MultiPrintQuery multi = queryBldr.getPrint();

        final SelectBuilder selTemplInst = SelectBuilder.get()
                        .linkto(CIPayroll.Template2EmployeeAbstract.FromAbstractLink).instance();
        final SelectBuilder selEmplInst = SelectBuilder.get()
                        .linkto(CIPayroll.Template2EmployeeAbstract.ToAbstractLink).instance();
        multi.addSelect(selTemplInst, selEmplInst);
        multi.execute();

        final Object[] rateObj = new Object[] { BigDecimal.ONE, BigDecimal.ONE };
        final String date = _parameter.getParameterValue(CIFormPayroll.Payroll_AdvanceCreateMultipleForm.date.name);
        final Dimension timeDim = CIPayroll.Payslip.getType().getAttribute(CIPayroll.Payslip.LaborTime.name)
                        .getDimension();
        while (multi.next()) {
            final Instance templInst = multi.getSelect(selTemplInst);
            final Instance emplInst = multi.getSelect(selEmplInst);

            final CreatedDoc createdDoc = new CreatedDoc();

            final Insert insert = new Insert(CIPayroll.Advance);
            insert.add(CIPayroll.Advance.Name, getDocName4Create(_parameter));
            insert.add(CIPayroll.Advance.Date, date);
            insert.add(CIPayroll.Advance.EmployeeAbstractLink, emplInst);
            insert.add(CIPayroll.Advance.Status, Status.find(CIPayroll.AdvanceStatus.Draft));
            insert.add(CIPayroll.Advance.RateCrossTotal, BigDecimal.ZERO);
            insert.add(CIPayroll.Advance.RateNetTotal, BigDecimal.ZERO);
            insert.add(CIPayroll.Advance.Rate, rateObj);
            insert.add(CIPayroll.Advance.CrossTotal, BigDecimal.ZERO);
            insert.add(CIPayroll.Advance.NetTotal, BigDecimal.ZERO);
            insert.add(CIPayroll.Advance.DiscountTotal, 0);
            insert.add(CIPayroll.Advance.RateDiscountTotal, 0);
            insert.add(CIPayroll.Advance.AmountCost, BigDecimal.ZERO);
            insert.add(CIPayroll.Advance.CurrencyId, Currency.getBaseCurrency());
            insert.add(CIPayroll.Advance.RateCurrencyId, Currency.getBaseCurrency());
            insert.add(CIPayroll.Advance.LaborTime, new Object[] { 120, timeDim.getBaseUoM().getId() });
            insert.add(CIPayroll.Advance.ExtraLaborTime, new Object[] { 0, timeDim.getBaseUoM().getId() });
            insert.add(CIPayroll.Advance.NightLaborTime, new Object[] { 0, timeDim.getBaseUoM().getId() });
            insert.add(CIPayroll.Advance.HolidayLaborTime, new Object[] { 0, timeDim.getBaseUoM().getId() });
            insert.add(CIPayroll.Advance.TemplateLinkAbstract, templInst);
            insert.execute();

            createdDoc.setInstance(insert.getInstance());

            final List<? extends AbstractRule<?>> rules = Template.getRules4Template(_parameter, templInst);
            Calculator.evaluate(_parameter, rules, createdDoc.getInstance());
            final Result result = Calculator.getResult(_parameter, rules);
            new Payslip().updateTotals(_parameter, insert.getInstance(), result, Currency.getBaseCurrency(), rateObj);
        }

        return new Return();
    }

    public Return edit4Multiple(final Parameter _parameter)
        throws EFapsException
    {
        final String selected = _parameter.getParameterValue("selected");
        final String[] oids = _parameter.getParameterValues("oid");
        final String[] values = _parameter.getParameterValues("newValue");
        if (oids != null && values != null && oids.length == values.length) {
            for (int i = 0; i < values.length; i++) {
                final Instance tmpInst = Instance.get(oids[i]);
                final Instance docInst;
                final Map<Instance, BigDecimal> mapping = new HashMap<>();
                if (tmpInst.getType().isKindOf(CIPayroll.PositionAbstract)) {
                    final PrintQuery print = new PrintQuery(tmpInst);
                    final SelectBuilder selDocInst = SelectBuilder.get()
                                    .linkto(CIPayroll.PositionAbstract.DocumentAbstractLink).instance();
                    print.addSelect(selDocInst);
                    print.execute();
                    docInst = print.getSelect(selDocInst);
                    try {
                        final BigDecimal amount = (BigDecimal) NumberFormatter.get().getTwoDigitsFormatter()
                                        .parse(values[i]);
                        mapping.put(tmpInst, amount);
                    } catch (final ParseException e) {
                        LOG.error("Catched ParserException", e);
                    }
                } else {
                    docInst = tmpInst;
                    try {
                        final BigDecimal amount = (BigDecimal) NumberFormatter.get().getTwoDigitsFormatter()
                                        .parse(values[i]);
                        mapping.put(Instance.get(selected), amount);
                    } catch (final ParseException e) {
                        LOG.error("Catched ParserException", e);
                    }
                }
                final PrintQuery print = new PrintQuery(docInst);
                final SelectBuilder selRateCurInst = SelectBuilder.get().linkto(CIPayroll.Payslip.RateCurrencyId)
                                .instance();
                print.addSelect(selRateCurInst);
                print.addAttribute(CIPayroll.Advance.Rate, CIPayroll.Advance.Date,
                                CIPayroll.Advance.LaborTime, CIPayroll.Advance.HolidayLaborTime,
                                CIPayroll.Advance.ExtraLaborTime, CIPayroll.Advance.NightLaborTime);
                print.execute();

                final Instance rateCurrInst = print.getSelect(selRateCurInst);
                final Object[] rateObj = print.getAttribute(CIPayroll.Advance.Rate);
                final Object[] laborTime = print.getAttribute(CIPayroll.Advance.LaborTime);
                final Object[] extraLaborTime = print.getAttribute(CIPayroll.Advance.ExtraLaborTime);
                final Object[] holidayLaborTime = print.getAttribute(CIPayroll.Advance.HolidayLaborTime);
                final Object[] nightLaborTime = print.getAttribute(CIPayroll.Advance.NightLaborTime);

                final Update update = new Update(docInst);
                if (selected.equals("LaborTime")) {
                    update.add(CIPayroll.Advance.LaborTime, new Object[] { values[i], laborTime[1] });
                }
                if (selected.equals("ExtraLaborTime")) {
                    update.add(CIPayroll.Advance.ExtraLaborTime, new Object[] { values[i], extraLaborTime[1] });
                }
                if (selected.equals("HolidayLaborTime")) {
                    update.add(CIPayroll.Advance.HolidayLaborTime, new Object[] { values[i], holidayLaborTime[1] });
                }
                if (selected.equals("NightLaborTime")) {
                    update.add(CIPayroll.Advance.NightLaborTime, new Object[] { values[i], nightLaborTime[1] });
                }
                update.execute();

                final Payslip payslip = new Payslip();
                final List<? extends AbstractRule<?>> rules = payslip.analyseRulesFomDoc(_parameter, docInst, mapping);
                final Result result = Calculator.getResult(_parameter, rules);
                payslip.updateTotals(_parameter, docInst, result, rateCurrInst, rateObj);
            }
        }
        return new Return();
    }

    @Override
    protected Type getType4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return getCIType().getType();
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CIPayroll.Advance;
    }
}
