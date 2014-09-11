/*
 * Copyright 2003 - 2013 The eFaps Team
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

import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.builder.style.Styles;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsClassLoader;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
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
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.payroll.rules.AbstractRule;
import org.efaps.esjp.payroll.rules.Calculator;
import org.efaps.esjp.payroll.rules.ExpressionRule;
import org.efaps.esjp.payroll.rules.InputRule;
import org.efaps.esjp.payroll.util.Payroll.RuleType;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.document.AbstractDocumentSum;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.esjp.ui.html.Table;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f1c7d3d5-23d2-4d72-a5d4-3c811a159062")
@EFapsRevision("$Rev$")
public abstract class Payslip_Base
    extends AbstractDocumentSum
{

    /**
     * Logger for this classes.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(Payslip_Base.class);


    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing map for autocomplete
     * @throws EFapsException on error
     */
    public Return autoComplete4Rule(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
        queryBldr.addWhereAttrMatchValue(CIPayroll.RuleAbstract.Key, input + "*").setIgnoreCase(true);
        InterfaceUtils.addMaxResult2QueryBuilder4AutoComplete(_parameter, queryBldr);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.RuleAbstract.Key,
                        CIPayroll.RuleAbstract.Description);
        multi.execute();
        final Map<String, Map<String, Object>> sortMap = new TreeMap<>();
        while (multi.next()) {
            final String key = multi.getAttribute(CIPayroll.RuleAbstract.Key);
            final String descr = multi.getAttribute(CIPayroll.RuleAbstract.Description);
            final String oid = multi.getCurrentInstance().getOid();
            final Map<String, Object> map = new HashMap<>();
            final String choice = key + " - " + descr;
            map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), oid);
            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), key);
            map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), choice);
            sortMap.put(choice, map);
        }

        final List<Map<String, Object>> list = new ArrayList<>();
        list.addAll(sortMap.values());
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing map for autocomplete
     * @throws EFapsException on error
     */
    public Return update4Rule(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();

        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
        final int idx = getSelectedRow(_parameter);
        if (field != null) {
            final Instance ruleInstance = Instance.get(_parameter.getParameterValues(field.getName())[idx]);
            final List<? extends AbstractRule<?>> rules = AbstractRule.getRules(ruleInstance);
            final AbstractRule<?> rule = rules.get(0);
            map.put(CITablePayroll.Payroll_PositionRuleTable.ruleDescription.name, rule.getDescription());
            list.add(map);
        }
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * Create a payslip.
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final String name = getDocName4Create(_parameter);

        final String date = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.date.name);
        final String dueDate = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.dueDate.name);
        final Long employeeid = Instance.get(
                        _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.employee.name)).getId();
        final String laborTimes = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.laborTime.name);
        final String laborTimeUoMs = _parameter.getParameterValue("laborTimeUoM");
        final String extraLaborTimes = _parameter
                        .getParameterValue(CIFormPayroll.Payroll_PayslipForm.extraLaborTime.name);
        final String extraLaborTimeUoMs = _parameter.getParameterValue("extraLaborTimeUoM");
        final String amount2Pay = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.rateCrossTotal .name);
        final String amountCosts = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.amountCost.name);
        final String currencyLinks = _parameter.getParameterValue(
                        CIFormPayroll.Payroll_PayslipForm.rateCurrencyId.name);
        final String movementType = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.movementType.name);
        try {
            final DecimalFormat formater =  NumberFormatter.get().getTwoDigitsFormatter();

            final Instance baseCurIns = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);

            final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(), currencyLinks);

            final PriceUtil util = new PriceUtil();
            final BigDecimal[] rates = util.getExchangeRate(util.getDateFromParameter(_parameter), rateCurrInst);
            final CurrencyInst cur = new CurrencyInst(rateCurrInst);
            final Object[] rate = new Object[] { cur.isInvert() ? BigDecimal.ONE : rates[1],
                            cur.isInvert() ? rates[1] : BigDecimal.ONE };
            final BigDecimal ratePay = amount2Pay != null && !amount2Pay.isEmpty()
                                                        ? (BigDecimal) formater.parse(amount2Pay)
                                                        : BigDecimal.ZERO;
            final BigDecimal rateCost = amountCosts != null && !amountCosts.isEmpty()
                                                        ? (BigDecimal) formater.parse(amountCosts)
                                                        : BigDecimal.ZERO;
            final BigDecimal time = laborTimes != null && !laborTimes.isEmpty()
                                                        ? (BigDecimal) formater.parse(laborTimes)
                                                        : BigDecimal.ZERO;
            final BigDecimal extraTime = extraLaborTimes != null && !extraLaborTimes.isEmpty()
                                                        ? (BigDecimal) formater.parse(extraLaborTimes)
                            : BigDecimal.ZERO;
            final BigDecimal pay = ratePay.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : ratePay
                            .setScale(8, BigDecimal.ROUND_HALF_UP).divide(rates[0], BigDecimal.ROUND_HALF_UP)
                            .setScale(2, BigDecimal.ROUND_HALF_UP);
            final BigDecimal cost = rateCost.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : rateCost
                            .setScale(8, BigDecimal.ROUND_HALF_UP).divide(rates[0], BigDecimal.ROUND_HALF_UP)
                            .setScale(2, BigDecimal.ROUND_HALF_UP);

            final Insert insert = new Insert(CIPayroll.Payslip);
            insert.add(CIPayroll.Payslip.Name, name);
            insert.add(CIPayroll.Payslip.Date, date);
            insert.add(CIPayroll.Payslip.DueDate, dueDate);
            insert.add(CIPayroll.Payslip.EmployeeAbstractLink, employeeid);
            insert.add(CIPayroll.Payslip.StatusAbstract, Status.find(CIPayroll.PayslipStatus.Draft));
            insert.add(CIPayroll.Payslip.RateCrossTotal, ratePay);
            insert.add(CIPayroll.Payslip.RateNetTotal, ratePay);
            insert.add(CIPayroll.Payslip.Rate, rate);
            insert.add(CIPayroll.Payslip.CrossTotal, pay);
            insert.add(CIPayroll.Payslip.NetTotal, pay);
            insert.add(CIPayroll.Payslip.DiscountTotal, 0);
            insert.add(CIPayroll.Payslip.RateDiscountTotal, 0);
            insert.add(CIPayroll.Payslip.AmountCost, cost);
            insert.add(CIPayroll.Payslip.CurrencyId, baseCurIns.getId());
            insert.add(CIPayroll.Payslip.RateCurrencyId, currencyLinks);
            insert.add(CIPayroll.Payslip.LaborTime, new Object[] { time, laborTimeUoMs });
            insert.add(CIPayroll.Payslip.ExtraLaborTime, new Object[] { extraTime, extraLaborTimeUoMs });
            insert.add(CIPayroll.Payslip.MovementType, movementType);
            insert.execute();

            final List<? extends AbstractRule<?>> rules = analyseRulesFomUI(_parameter, getRuleInstFromUI(_parameter));
            updatePositions(_parameter, insert.getInstance(), rules);

        } catch (final ParseException e) {
            throw new EFapsException(Payslip_Base.class, "create.ParseException", e);
        }

        return ret;
    }

    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();

        final List<? extends AbstractRule<?>> rules = analyseRulesFomUI(_parameter, getRuleInstFromUI(_parameter));
        updatePositions(_parameter, instance, rules);
        return new Return();
    }


    protected void updatePositions(final Parameter _parameter,
                                   final Instance _docInst,
                                   final List<? extends AbstractRule<?>> _rules)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink, _docInst);
        final InstanceQuery query = queryBldr.getQuery();
        final List<Instance> posInsts = query.executeWithoutAccessCheck();
        final Iterator<Instance> posIter = posInsts.iterator();
        int idx = 1;
        for (final AbstractRule<?> rule : _rules) {
            CIType ciType;
            switch (rule.getRuleType()) {
                case PAYMENT:
                    ciType = CIPayroll.PositionPayment;
                    break;
                case DEDUCTION:
                    ciType = CIPayroll.PositionDeduction;
                    break;
                case NEUTRAL:
                    ciType = CIPayroll.PositionNeutral;
                    break;
                default:
                    ciType = CIPayroll.PositionSum;
            }

            Update update;
            Instance posInst = null;
            if (posIter.hasNext()) {
                posInst = posIter.next();

                if (posInst.getType().isCIType(ciType)) {
                    update = new Update(posInst);
                } else {
                    update = new Insert(ciType);
                    update.add(CIPayroll.PositionAbstract.DocumentAbstractLink, _docInst);
                    new Delete(posInst).execute();
                }
            } else {
                update = new Insert(ciType);
                update.add(CIPayroll.PositionAbstract.DocumentAbstractLink, _docInst);
            }

            final Instance baseCurIns = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);

            update.add(CIPayroll.PositionAbstract.RuleAbstractLink, rule.getInstance());
            update.add(CIPayroll.PositionAbstract.PositionNumber, idx);
            update.add(CIPayroll.PositionAbstract.Description, rule.getDescription());
            update.add(CIPayroll.PositionAbstract.Key, rule.getKey());
            update.add(CIPayroll.PositionAbstract.RateAmount, rule.getResult());
            update.add(CIPayroll.PositionAbstract.Rate, new Object[]{ BigDecimal.ONE, BigDecimal.ONE });
            update.add(CIPayroll.PositionAbstract.Amount, BigDecimal.ONE);
            update.add(CIPayroll.PositionAbstract.CurrencyLink, baseCurIns);
            update.add(CIPayroll.PositionAbstract.RateCurrencyLink,baseCurIns);
            update.execute();

            idx++;
        }

        while (posIter.hasNext()) {
            new Delete(posIter.next()).execute();
        }
    }


    /**
     * Create massive payslips.
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return createMassive(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String name = _parameter.getParameterValue("name");
        final String date = _parameter.getParameterValue("date");
        final String dueDate = _parameter.getParameterValue("dueDate");

        final String[] employees = _parameter.getParameterValues(CITablePayroll.Payroll_PayslipTable.employee.name);
        final String[] employeeNames = _parameter.getParameterValues(CITablePayroll.Payroll_PayslipTable.employee.name
                        + "AutoComplete");
        final String[] laborTimes = _parameter.getParameterValues(CITablePayroll.Payroll_PayslipTable.laborTime.name);
        final String[] laborTimeUoMs = _parameter.getParameterValues(CITablePayroll.Payroll_PayslipTable.laborTime.name
                        + "UoM");
        final String[] amount2Pays = _parameter
                        .getParameterValues(CITablePayroll.Payroll_PayslipTable.rateCrossTotal.name);
        final String[] amountCosts = _parameter.getParameterValues(CITablePayroll.Payroll_PayslipTable.amountCost.name);
        final String[] currencyLinks = _parameter
                        .getParameterValues(CITablePayroll.Payroll_PayslipTable.rateCurrencyId.name);
        final DecimalFormat formater =  NumberFormatter.get().getTwoDigitsFormatter();
        try {
            final Instance baseCurIns = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
            final PriceUtil util = new PriceUtil();

            for (int i = 0; i < employees.length; i++) {
                if (employees[i] != null && !employees[i].isEmpty()
                                && laborTimes[i] != null && !laborTimes[i].isEmpty()) {

                    final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(), currencyLinks[i]);

                    final BigDecimal[] rates = util
                                    .getExchangeRate(util.getDateFromParameter(_parameter), rateCurrInst);
                    final CurrencyInst cur = new CurrencyInst(rateCurrInst);
                    final Object[] rate = new Object[] { cur.isInvert() ? BigDecimal.ONE : rates[1],
                                    cur.isInvert() ? rates[1] : BigDecimal.ONE };

                    final BigDecimal ratePay = amount2Pays[i] != null && !amount2Pays[i].isEmpty()
                                    ? (BigDecimal) formater.parse(amount2Pays[i])
                                    : BigDecimal.ZERO;
                    final BigDecimal rateCost = amountCosts[i] != null && !amountCosts[i].isEmpty()
                                    ? (BigDecimal) formater.parse(amountCosts[i])
                                    : BigDecimal.ZERO;
                    final BigDecimal time = laborTimes[i] != null && !laborTimes[i].isEmpty()
                                    ? (BigDecimal) formater.parse(laborTimes[i])
                                    : BigDecimal.ZERO;
                    final BigDecimal pay = ratePay.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : ratePay
                                    .setScale(8, BigDecimal.ROUND_HALF_UP).divide(rates[0], BigDecimal.ROUND_HALF_UP)
                                    .setScale(2, BigDecimal.ROUND_HALF_UP);
                    final BigDecimal cost = rateCost.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : rateCost
                                    .setScale(8, BigDecimal.ROUND_HALF_UP).divide(rates[0], BigDecimal.ROUND_HALF_UP)
                                    .setScale(2, BigDecimal.ROUND_HALF_UP);

                    final Insert insert = new Insert(CIPayroll.Payslip);
                    insert.add(CIPayroll.Payslip.Name, name + " " + employeeNames[i]);
                    insert.add(CIPayroll.Payslip.Date, date);
                    insert.add(CIPayroll.Payslip.DueDate, dueDate);
                    insert.add(CIPayroll.Payslip.EmployeeAbstractLink, Instance.get(employees[i]).getId());
                    insert.add(CIPayroll.Payslip.StatusAbstract,
                                    ((Long) Status.find(CIPayroll.PayslipStatus.uuid, "Open").getId()).toString());
                    insert.add(CIPayroll.Payslip.RateCrossTotal, ratePay);
                    insert.add(CIPayroll.Payslip.RateNetTotal, ratePay);
                    insert.add(CIPayroll.Payslip.Rate, rate);
                    insert.add(CIPayroll.Payslip.CrossTotal, pay);
                    insert.add(CIPayroll.Payslip.NetTotal, pay);
                    insert.add(CIPayroll.Payslip.DiscountTotal, 0);
                    insert.add(CIPayroll.Payslip.RateDiscountTotal, 0);
                    insert.add(CIPayroll.Payslip.AmountCost, cost);
                    insert.add(CIPayroll.Payslip.CurrencyId, baseCurIns.getId());
                    insert.add(CIPayroll.Payslip.RateCurrencyId, rateCurrInst.getId());
                    insert.add(CIPayroll.Payslip.LaborTime, new Object[] { time, laborTimeUoMs[i] });
                    insert.add(CIPayroll.Payslip.ExtraLaborTime, new Object[] { 0, laborTimeUoMs[i] });
                    insert.execute();
                }
            }
        } catch (final ParseException e) {
            throw new EFapsException(Payslip_Base.class, "createMassive.ParseException", e);
        }
        return ret;
    }

    /**
     * Create advances.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return createAdvance(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue(CIFormPayroll.Payroll_AdvanceForm.date.name);

        final String[] employees = _parameter.getParameterValues(CITablePayroll.Payroll_AdvanceTable.employee.name);
        final String[] employeeNames = _parameter.getParameterValues(CITablePayroll.Payroll_AdvanceTable.employee.name
                        + "AutoComplete");
        final String[] amount2Pays = _parameter
                        .getParameterValues(CITablePayroll.Payroll_AdvanceTable.rateCrossTotal.name);
        final String[] currencyLinks = _parameter
                        .getParameterValues(CITablePayroll.Payroll_AdvanceTable.rateCurrencyId.name);
        final DecimalFormat formater =  NumberFormatter.get().getTwoDigitsFormatter();
        try {
            final Instance baseCurIns = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
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
                    insert.add(CIPayroll.Advance.Name,
                                    DBProperties.getProperty("org.efaps.esjp.payroll.Payslip.Advance") + " "
                                                    + employeeNames[i]);
                    insert.add(CIPayroll.Advance.Date, date);
                    insert.add(CIPayroll.Advance.EmployeeAbstractLink, Instance.get(employees[i]).getId());
                    insert.add(CIPayroll.Advance.Status,
                                    ((Long) Status.find(CIPayroll.AdvanceStatus.uuid, "Open").getId()).toString());
                    insert.add(CIPayroll.Advance.RateCrossTotal, ratePay);
                    insert.add(CIPayroll.Advance.RateNetTotal, ratePay);
                    insert.add(CIPayroll.Advance.CrossTotal, pay);
                    insert.add(CIPayroll.Advance.NetTotal, pay);
                    insert.add(CIPayroll.Advance.RateCurrencyId, rateCurrInst.getId());
                    insert.add(CIPayroll.Advance.CurrencyId, baseCurIns.getId());
                    insert.add(CIPayroll.Advance.AmountCost, pay);
                    insert.add(CIPayroll.Advance.Rate, rate);
                    insert.add(CIPayroll.Advance.DiscountTotal, 0);
                    insert.add(CIPayroll.Advance.RateDiscountTotal, 0);
                    insert.add(CIPayroll.Advance.LaborTime, new Object[] { 0,
                                    Dimension.get(UUID.fromString("8154e40c-3f2d-4bc0-91e6-b8510eaf642c"))
                                       .getBaseUoM().getId() });
                    insert.add(CIPayroll.Advance.ExtraLaborTime, new Object[] { 0,
                                    Dimension.get(UUID.fromString("8154e40c-3f2d-4bc0-91e6-b8510eaf642c"))
                                       .getBaseUoM().getId() });
                    insert.execute();
                }
            }
        } catch (final ParseException e) {
            throw new EFapsException(Payslip_Base.class, "createMassive.ParseException", e);
        }
        return new Return();
    }

    /**
     * Method to set the instance of the payslip selected to the Context.
     *
     * @param _parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error
     */
    @Override
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final String oid = _parameter.getParameterValue("selectedRow");
        final Instance inst = Instance.get(oid);
        Context.getThreadContext().setSessionAttribute("payslip", inst);
        final StringBuilder js = new StringBuilder();

        js.append("<script type=\"text/javascript\">");
        if (inst != null && inst.isValid()) {
            js.append(getSetValuesString(_parameter, inst));
        }
        js.append("</script>");
        final Return ret = new Return();
        ret.put(ReturnValues.SNIPLETT, js.toString());
        return ret;
    }

    /**
     * Method to get the javascript part for setting the values.
     *
     * @param _instance instance to be copied
     * @return javascript
     * @throws EFapsException on error
     */
    @Override
    protected String getSetValuesString(final Parameter _parameter,
                                        final Instance _instance)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        final PrintQuery print = new PrintQuery(_instance);
        print.addAttribute(CIPayroll.Payslip.Name,
                        CIPayroll.Payslip.Note,
                        CIPayroll.Payslip.LaborTime,
                        CIPayroll.Payslip.ExtraLaborTime,
                        CIPayroll.Payslip.RateCrossTotal,
                        CIPayroll.Payslip.AmountCost,
                        CIPayroll.Payslip.RateCurrencyId);
        final SelectBuilder selEmpOID = new SelectBuilder().linkto(CIPayroll.Payslip.EmployeeAbstractLink).oid();
        final SelectBuilder selEmpNum = new SelectBuilder().linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                        .attribute(CIHumanResource.Employee.Number);
        final SelectBuilder selEmpLastName = new SelectBuilder().linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                        .attribute(CIHumanResource.Employee.LastName);
        final SelectBuilder selEmpFirstName = new SelectBuilder().linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                        .attribute(CIHumanResource.Employee.FirstName);
        print.addSelect(selEmpOID, selEmpLastName, selEmpFirstName, selEmpNum);
        print.execute();

        print.<BigDecimal>getAttribute(CIPayroll.Payslip.RateCrossTotal);
        print.<BigDecimal>getAttribute(CIPayroll.Payslip.AmountCost);
        final String empOid = print.<String>getSelect(selEmpOID);
        final String empNum = print.<String>getSelect(selEmpNum);
        final String empLName = print.<String>getSelect(selEmpLastName);
        final String empFName = print.<String>getSelect(selEmpFirstName);
        final Object[] laborTime = print.getAttribute(CIPayroll.Payslip.LaborTime);
        final BigDecimal laborTimeVal = (BigDecimal) laborTime[0];
        final UoM laborTimeUoM = (UoM) laborTime[1];
        final Object[] extraLaborTime = print.getAttribute(CIPayroll.Payslip.ExtraLaborTime);
        final BigDecimal extraLaborTimeVal = (BigDecimal) extraLaborTime[0];
        final UoM extraLaborTimeUoM = (UoM) extraLaborTime[1];

        final Long curId = print.<Long>getAttribute(CIPayroll.Payslip.RateCurrencyId);

        final DecimalFormat formater =  NumberFormatter.get().getTwoDigitsFormatter();

        js.append("function setValue() {\n")
            .append("document.getElementsByName('").append(CIFormPayroll.Payroll_PayslipForm.rateCurrencyId.name)
            .append("')[0].value=").append(curId).append(";\n")
            .append(getSetFieldValue(0, "number", empOid, empNum)).append("\n")
            .append(getSetFieldValue(0, "employeeData", empLName + ", " + empFName)).append("\n")
            .append(getSetFieldValue(0, "laborTime", formater.format(laborTimeVal))).append("\n")
            .append("document.getElementsByName('laborTimeUoM')[0].value=")
            .append(laborTimeUoM.getId()).append(";")
            .append(getSetFieldValue(0, "extraLaborTime", formater.format(extraLaborTimeVal))).append("\n")
            .append("document.getElementsByName('extraLaborTimeUoM')[0].value=")
            .append(extraLaborTimeUoM.getId()).append(";\n");


        js.append("}\n")
            .append("Wicket.Event.add(window, \"domready\", function(event) {\n")
            .append(" });");
        return js.toString();
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _docInst      instance of the document to be updated
     * @param _list         list of positions
     * @param _rates        Rates
     * @throws EFapsException on error
     */
    protected void setAmounts(final Parameter _parameter,
                              final Instance _docInst,
                              final List<InsertPos> _list,
                              final BigDecimal[] _rates)
        throws EFapsException
    {
        BigDecimal ratePay = BigDecimal.ZERO;
        BigDecimal amountCost = BigDecimal.ZERO;
        for (final InsertPos pos : _list) {
            if (pos.caseType.equals(CIPayroll.CasePositionRootSum.getType())) {
                ratePay = ratePay.add(pos.getAmount());
            } else if (pos.posType.equals(CIPayroll.PositionNeutral.getType())
                            || pos.posType.equals(CIPayroll.PositionPayment.getType())) {
                amountCost = amountCost.add(pos.getAmount());
            }
        }
        final BigDecimal pay = ratePay.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : ratePay
                        .setScale(8, BigDecimal.ROUND_HALF_UP).divide(_rates[0], BigDecimal.ROUND_HALF_UP)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
        final BigDecimal cost = amountCost.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : amountCost
                        .setScale(8, BigDecimal.ROUND_HALF_UP).divide(_rates[0], BigDecimal.ROUND_HALF_UP)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
        final Update update = new Update(_docInst);
        update.add(CIPayroll.Payslip.RateCrossTotal, ratePay);
        update.add(CIPayroll.Payslip.RateNetTotal, ratePay);
        update.add(CIPayroll.Payslip.CrossTotal, pay);
        update.add(CIPayroll.Payslip.NetTotal, pay);
        update.add(CIPayroll.Payslip.AmountCost, cost);
        update.execute();
    }

    /**
     * Method for return a first date of month and last date of month.
     * @param _parameter Parameter as passed from eFaps API.
     * @return ret Return.
     * @throws EFapsException on error.
     */
    public Return dateValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final FieldValue fValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final DateTime value;
        if (fValue.getTargetMode().equals(TargetMode.CREATE)) {
            if (fValue.getField().getName().equals("date")) {
                value = new DateTime().dayOfMonth().withMinimumValue();
            } else {
                value = new DateTime().dayOfMonth().withMaximumValue();
            }
        } else {
            value = (DateTime) fValue.getValue();
        }
        ret.put(ReturnValues.VALUES, value);
        return ret;
    }

    /**
     * Method to get the value for the field directly under the Employee.
     *
     * @param _instance Instacne of the contact
     * @return String for the field
     * @throws EFapsException on error
     */
    protected String getFieldValue4Employee(final Instance _instance)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_instance);
        print.addAttribute(CIHumanResource.EmployeeAbstract.FirstName, CIHumanResource.EmployeeAbstract.LastName);
        final SelectBuilder selSecNumb = new SelectBuilder()
                .clazz(CIHumanResource.ClassTR_Health)
                .attribute(CIHumanResource.ClassTR_Health.CUSPP);
        final SelectBuilder selSec = new SelectBuilder()
                .clazz(CIHumanResource.ClassTR_Health)
                .linkto(CIHumanResource.ClassTR_Health.PensionRegimeLink)
                .attribute(CIHumanResource.AttributeDefinitionPensionRegime.Value);
        final SelectBuilder selSecDesc = new SelectBuilder()
                .clazz(CIHumanResource.ClassTR_Health)
                .linkto(CIHumanResource.ClassTR_Health.InsuranceLink)
                .attribute(CIHumanResource.AttributeDefinitionInsurance.Description);
        print.addSelect(selSec, selSecNumb, selSecDesc);
        print.execute();
        final String firstname = print.<String>getAttribute(CIHumanResource.EmployeeAbstract.FirstName);
        final String lastname = print.<String>getAttribute(CIHumanResource.EmployeeAbstract.LastName);
        final String sec = print.<String>getSelect(selSec);
        final String secDesc = print.<String>getSelect(selSecDesc);
        final String secNum = print.<String>getSelect(selSecNumb);

        final StringBuilder strBldr = new StringBuilder();
        strBldr.append(lastname).append(", ").append(firstname);
        if (sec != null && !sec.isEmpty()) {
            strBldr.append(" - ").append(sec).append("-").append(secDesc).append(": ").append(secNum);
        }
        return strBldr.toString();
    }

    /**
     * Method to update the fields on leaving the employee field.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return updateFields4Employee(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();

        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);

        if (field != null) {
            final Instance instance = Instance.get(_parameter.getParameterValue(field.getName()));

            if (instance.getId() > 0) {
                map.put("employeeData", getFieldValue4Employee(instance));
            } else {
                map.put("employeeData", "????");
            }
            list.add(map);
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Executed the command on the button.
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return executeButton(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance templInst = Instance.get(_parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.template.name));
        if (templInst.isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Template2Rule);
            queryBldr.addWhereAttrEqValue(CIPayroll.Template2Rule.FromLink, templInst);
            queryBldr.addOrderByAttributeAsc(CIPayroll.Template2Rule.Sequence);

            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder ruleInst = SelectBuilder.get().linkto(CIPayroll.Template2Rule.ToLink).instance();
            multi.addSelect(ruleInst);
            multi.setEnforceSorted(true);
            multi.execute();

            final List<Instance> ruleInsts = new ArrayList<>();
            while (multi.next()) {
                ruleInsts.add(multi.<Instance>getSelect(ruleInst));
            }
            final List<? extends AbstractRule<?>> rules = AbstractRule.getRules(ruleInsts.toArray(new Instance[ruleInsts
                            .size()]));
            final Collection<Map<String, Object>> values = new ArrayList<>();
            for (final AbstractRule<?> rule : rules) {
                final Map<String, Object> map = new HashMap<>();
                values.add(map);
                map.put(CITablePayroll.Payroll_PositionRuleTable.rulePosition.name,
                                new String[] {rule.getInstance().getOid(), rule.getKey()});
                map.put(CITablePayroll.Payroll_PositionRuleTable.ruleDescription.name, rule.getDescription());
            }
            final StringBuilder js = getTableRemoveScript(_parameter, "ruleTable")
                               .append(getTableAddNewRowsScript(_parameter, "ruleTable", values, null));
            ret.put(ReturnValues.SNIPLETT, js.toString());
        }
        return ret;
    }


    /**
     * @param _parameter as passed from eFaps API.
     * @return Map with positions
     * @throws EFapsException on error.
     */
    protected Map<Instance, Position> analysePositions(final Parameter _parameter)
        throws EFapsException
    {
        final Map<Instance, Position> ret = new HashMap<Instance, Position>();

        final String caseOid = _parameter.getParameterValue("case");
        final Instance caseInst = Instance.get(caseOid);
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.CasePositionRootSum);
        queryBldr.addWhereAttrEqValue(CIPayroll.CasePositionSumAbstract.CaseAbstractLink, caseInst.getId());

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.CasePositionAbstract.Name,
                            CIPayroll.CasePositionAbstract.Description,
                            CIPayroll.CasePositionAbstract.Sorted,
                            CIPayroll.CasePositionAbstract.Mode);
        multi.execute();
        while (multi.next()) {
            final String name = multi.<String>getAttribute(CIPayroll.CasePositionAbstract.Name);
            final String description = multi.<String>getAttribute(CIPayroll.CasePositionAbstract.Description);
            final Integer sorted = multi.<Integer>getAttribute(CIPayroll.CasePositionAbstract.Sorted);
            final Integer mode = multi.<Integer>getAttribute(CIPayroll.CasePositionAbstract.Mode);
            final SumPosition sum = new SumPosition(ret, multi.getCurrentInstance(), name, description, sorted, mode);
            ret.put(multi.getCurrentInstance(), sum);
        }
        return ret;
    }

    /**
     * Method to update the fields on leaving a amount field.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return update4Amount(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<Instance, Integer> ruleInsts = getRuleInstFromUI(_parameter);
        final List<? extends AbstractRule<?>> rules = analyseRulesFomUI(_parameter, ruleInsts);

        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();
        final String html = Calculator.getHtml4Rules(_parameter, rules) + "<br/>"
                        + getHtml4Positions(_parameter, rules);
        final StringBuilder js = new StringBuilder().append("document.getElementsByName('sums')[0].innerHTML='")
                        .append(StringEscapeUtils.escapeEcmaScript(html)).append("';");
        map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        list.add(map);

        for (final AbstractRule<?> rule : rules) {
            if (rule instanceof ExpressionRule) {
                final Map<String, Object> map2 = new HashMap<>();
                list.add(map2);
                map2.put(EFapsKey.FIELDUPDATE_USEIDX.getKey(), ruleInsts.get(rule.getInstance()));
                map2.put(CITablePayroll.Payroll_PositionRuleTable.ruleAmount.name, rule.getResult());
            }
        }

        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    protected List<? extends AbstractRule<?>> analyseRulesFomUI(final Parameter _parameter,
                                                                final Map<Instance, Integer> _ruleInsts)
        throws EFapsException
    {
        final String[] amounts = _parameter
                        .getParameterValues(CITablePayroll.Payroll_PositionRuleTable.ruleAmount.name);
        final List<? extends AbstractRule<?>> ret = AbstractRule.getRules(_ruleInsts.keySet().toArray(
                        new Instance[_ruleInsts.size()]));
        for (final AbstractRule<?> rule : ret) {
            if (rule instanceof InputRule) {
                rule.setExpression(amounts[_ruleInsts.get(rule.getInstance())]);
            }
        }
        Calculator.evaluate(_parameter, ret);
        return ret;
    }


    protected CharSequence getHtml4Positions(final Parameter _parameter,
                                             final List<? extends AbstractRule<?>> rules)
        throws EFapsException
    {
        final Table table = new Table();
        table.addRow().addColumn(CIPayroll.PositionPayment.getType().getLabel())
            .getCurrentColumn().setStyle("font-weight: bold;");
        for (final AbstractRule<?> rule : rules) {
            if (RuleType.PAYMENT.equals(rule.getRuleType())) {
                table.addRow().addColumn(rule.getKey()).addColumn(rule.getDescription())
                                .addColumn(String.valueOf(rule.getResult()));
            }
        }
        table.addRow().addColumn(CIPayroll.PositionDeduction.getType().getLabel())
            .getCurrentColumn().setStyle("font-weight: bold;");
        for (final AbstractRule<?> rule : rules) {
            if (RuleType.DEDUCTION.equals(rule.getRuleType())) {
                table.addRow().addColumn(rule.getKey()).addColumn(rule.getDescription())
                                .addColumn(String.valueOf(rule.getResult()));
            }
        }
        table.addRow().addColumn(CIPayroll.PositionNeutral.getType().getLabel())
            .getCurrentColumn().setStyle("font-weight: bold;");
        for (final AbstractRule<?> rule : rules) {
            if (RuleType.NEUTRAL.equals(rule.getRuleType())) {
                table.addRow().addColumn(rule.getKey()).addColumn(rule.getDescription())
                                .addColumn(String.valueOf(rule.getResult()));
            }
        }
        table.addRow().addColumn(CIPayroll.PositionSum.getType().getLabel())
            .getCurrentColumn().setStyle("font-weight: bold;");
        for (final AbstractRule<?> rule : rules) {
            if (RuleType.SUM.equals(rule.getRuleType())) {
                table.addRow().addColumn(rule.getKey()).addColumn(rule.getDescription())
                                .addColumn(String.valueOf(rule.getResult()));
            }
        }
        return table.toHtml();
    }


    protected Map<Instance, Integer> getRuleInstFromUI(final Parameter _parameter)
    {
        final Map<Instance, Integer> ret = new LinkedHashMap<>();
        final String[] oids = _parameter.getParameterValues(CITablePayroll.Payroll_PositionRuleTable.rulePosition.name);

        if (oids != null) {
            int idx = 0;
            for (final String oid : oids) {
                final Instance ruleInstance = Instance.get(oid);
                if (ruleInstance.isValid()) {
                    ret.put(ruleInstance, idx);
                }
                idx++;
            }
        }
        return ret;
    }


    /**
     * Get the values from the tables.
     * @param _parameter    Parameter as passed from the eFaps API
     * @param _values       values to be analysed
     * @param _postfix      postfix of the table
     * @throws EFapsException on error.
     */
    protected void analyseTables(final Parameter _parameter,
                                 final Map<Instance, TablePos> _values,
                                 final String _postfix)
        throws EFapsException
    {
        try {
            final String[] posDed = _parameter.getParameterValues("casePosition_" + _postfix);
            final String[] amountDed = _parameter.getParameterValues("amount_" + _postfix);
            final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
            if (posDed != null) {
                for (int i = 0; i < posDed.length; i++) {
                    if (posDed[i] != null && !posDed[i].isEmpty()) {
                        final Instance inst = Instance.get(posDed[i]);
                        if (inst.isValid()) {
                            BigDecimal amount = BigDecimal.ZERO;
                            if (amountDed[i] != null && !amountDed[i].isEmpty()) {
                                amount = (BigDecimal) formater.parse(amountDed[i]);
                            }
                            if (_values.containsKey(inst)) {
                                _values.get(inst).add(amount, i);
                            } else {
                                _values.put(inst, new TablePos(amount, i, _postfix));
                            }

                        }
                    }
                }
            }
        } catch (final ParseException e) {
            throw new EFapsException(Payslip.class, "", e);
        }
    }

    /**
     * Method to create a report for the accounting actions definition, related with the payslip cases.
     *
     * @param _parameter as passed from eFaps API.
     * @return Return with the report.
     * @throws EFapsException on error.
     */
    public Return createActionReport(final Parameter _parameter)
        throws EFapsException
    {

        final StandartReport report = new StandartReport()
        {

            @Override
            public Return execute(final Parameter _parameter)
                throws EFapsException
            {
                final Return ret = new Return();
                final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);

                final String mime = (String) properties.get("Mime");
                final boolean print = "pdf".equalsIgnoreCase(mime);
                setFileName("ActionReport");

                final JasperReportBuilder jrb = DynamicReports
                                .report()
                                .addTitle(DynamicReports.cmp.horizontalList(
                                                DynamicReports.cmp.text("ActionReport"),
                                                DynamicReports.cmp.text(new Date())
                                                        .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                                                        .setDataType(DynamicReports.type.dateYearToMinuteType())));
                if (print) {
                    jrb.setPageMargin(DynamicReports.margin(20))
                                    .setPageFormat(PageType.A4, PageOrientation.LANDSCAPE)
                                    .setColumnHeaderStyle(DynamicReports.stl.style()
                                                    .setFont(Styles.font().bold().setFontSize(12))
                                                    .setBorder(Styles.pen1Point())
                                                    .setBackgroundColor(Color.gray)
                                                    .setForegroundColor(Color.white))
                                    .highlightDetailEvenRows()
                                    .pageFooter(DynamicReports.cmp.pageXofY().setStyle(DynamicReports.stl.style()
                                                    .setHorizontalAlignment(HorizontalAlignment.CENTER)));
                } else {
                    jrb.setIgnorePagination(true)
                                    .setPageMargin(DynamicReports.margin(0));
                }

                try {
                    final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup(
                                    "employee", String.class)
                                    .setTotalHeader("Total");
                    final CrosstabColumnGroupBuilder<String> colGroup = DynamicReports.ctab.columnGroup("action",
                                    String.class);

                    final CrosstabBuilder crosstab = DynamicReports.ctab
                                    .crosstab()
                                    .headerCell(DynamicReports.cmp.text("RR.HH. / Action"))
                                    .addColumnGroup(colGroup)
                                    .addRowGroup(rowGroup)
                                    .measures(DynamicReports.ctab.measure("amount", BigDecimal.class, Calculation.SUM));
                    jrb.summary(crosstab)
                        .setLocale(Context.getThreadContext().getLocale()).setDataSource(
                                    getActionReportDataSource(_parameter));

                    ret.put(ReturnValues.VALUES, super.getFile(jrb.toJasperPrint(), mime));

                    ret.put(ReturnValues.TRUE, true);
                } catch (final IOException e) {
                    throw new EFapsException(Payslip_Base.class, "IOException", e);
                } catch (final JRException e) {
                    throw new EFapsException(Payslip_Base.class, "IOException", e);
                } catch (final DRException e) {
                    throw new EFapsException(Payslip_Base.class, "IOException", e);
                }
                //
                return ret;
            }
        };
        return report.execute(_parameter);
    }


    protected JRDataSource getActionReportDataSource(final Parameter _parameter)
                    throws EFapsException
    {
        final DRDataSource ret = new DRDataSource("employee", "action", "amount");

        final DateTime dateTo = new DateTime(_parameter.getParameterValue("dateTo"));
        final DateTime dateFrom = new DateTime(_parameter.getParameterValue("dateFrom"));
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Payslip);
        queryBldr.addWhereAttrLessValue(CIPayroll.Payslip.Date, dateTo.plusDays(1));
        queryBldr.addWhereAttrGreaterValue(CIPayroll.Payslip.Date, dateFrom.minusSeconds(1));
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selLEmp = new SelectBuilder().linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                        .attribute(CIHumanResource.Employee.LastName);
        final SelectBuilder selFEmp = new SelectBuilder().linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                        .attribute(CIHumanResource.Employee.FirstName);
        multi.addSelect(selFEmp, selLEmp);
        multi.execute();
        while (multi.next()) {
            multi.<String>getSelect(selLEmp);
            multi.<String>getSelect(selFEmp);
            final QueryBuilder queryBldr2 = new QueryBuilder(CIPayroll.PositionAbstract);
            queryBldr2.addWhereAttrNotEqValue(CIPayroll.PositionAbstract.Type, CIPayroll.PositionSum.getType().getId());
            queryBldr2.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink,
                            multi.getCurrentInstance().getId());
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            multi2.addAttribute(CIPayroll.PositionAbstract.Amount);

        }
        return ret;
    }

    public class InsertPos
    {

        private final Type posType;
        private final BigDecimal amount;
        private final long id;
        private String description;
        private Integer sorted;
        private final Type caseType;

        /**
         * Getter method for the instance variable {@link #sorted}.
         *
         * @return value of instance variable {@link #sorted}
         */
        public Integer getSorted()
        {
            return this.sorted;
        }

        /**
         * @param values
         * @param _sums
         * @param _parameter
         * @param value
         * @throws EFapsException
         */
        public InsertPos(final Position _position,
                         final Parameter _parameter,
                         final Map<Instance, Position> _sums,
                         final Map<Instance, TablePos> _values)
            throws EFapsException
        {
            this.posType = CIPayroll.PositionSum.getType();
            this.caseType = _position.getInstance().getType();
            this.amount = _position.getResult(_parameter, _sums, _values);
            this.id = _position.getInstance().getId();
            setValues(_position.getInstance());
        }

        private void setValues(final Instance _instance)
            throws EFapsException
        {
            final PrintQuery print = new PrintQuery(_instance);
            print.addAttribute(CIPayroll.CasePositionAbstract.Description, CIPayroll.CasePositionAbstract.Sorted);
            print.execute();
            this.description = print.<String>getAttribute(CIPayroll.CasePositionAbstract.Description);
            this.sorted = print.<Integer>getAttribute(CIPayroll.CasePositionAbstract.Sorted);
         }

        /**
         * @param key
         * @param value
         * @throws EFapsException
         */
        public InsertPos(final Instance _instance,
                         final BigDecimal _amount)
            throws EFapsException
        {
            this.caseType = _instance.getType();
            if (_instance.getType().equals(CIPayroll.CasePositionDeduction.getType())) {
                this.posType = CIPayroll.PositionDeduction.getType();
            } else if (_instance.getType().equals(CIPayroll.CasePositionPayment.getType())) {
                this.posType = CIPayroll.PositionPayment.getType();
            } else {
                this.posType = CIPayroll.PositionNeutral.getType();
            }
            this.amount = _amount;
            this.id = _instance.getId();
            setValues(_instance);
        }

        /**
         * @return
         */
        public Type getType()
        {
            return this.posType;
        }

        /**
         * @return
         */
        public BigDecimal getAmount()
        {
            return this.amount;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    /**
     * A position in the currently displayed table.
     */
    public class TablePos
    {

        private final List<BigDecimal> values = new ArrayList<BigDecimal>();
        private final List<Integer> pos = new ArrayList<Integer>();

        private boolean setValue = false;
        public TablePos(final BigDecimal _amount,
                        final Integer _pos,
                        final String _postfix)

        {
            getValues().add(_amount);
            this.pos.add(_pos);
        }

        public void add(final BigDecimal _amount,
                        final Integer _pos)
        {
            getValues().add(_amount);
            this.pos.add(_pos);
        }

        /**
         * Getter method for the instance variable {@link #setValue}.
         *
         * @return value of instance variable {@link #setValue}
         */
        public boolean isSetValue()
        {
            return this.setValue;
        }

        /**
         * Setter method for instance variable {@link #setValue}.
         *
         * @param setValue value for instance variable {@link #setValue}
         */

        public void setSetValue(final boolean setValue)
        {
            this.setValue = setValue;
        }

        /**
         * Getter method for the instance variable {@link #values}.
         *
         * @return value of instance variable {@link #values}
         */
        public List<BigDecimal> getValues()
        {
            return this.values;
        }


        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    /**
     * Positions used for calculation.
     */
    public abstract class Position
    {
        /**
         * Instance of the caseposition.
         */
        private final Instance instance;

        /**
         * @param _instance instance of the caseposition
         */
        public Position(final Instance _instance)
        {
            this.instance = _instance;
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
         * @param _parameter    Parameter as passed by the eFaps API
         * @param _positions    Positions for calculation
         * @param _values       values in the form
         * @return
         */
        public abstract BigDecimal getResult(final Parameter _parameter,
                                             final Map<Instance, Position> _positions,
                                             final Map<Instance, TablePos> _values);

    }

    public class CalcPosition
        extends Position
    {

        private Instance relInst;
        private Integer denominator;
        private Integer numerator;
        private Instance toInst;
        private final String esjp;
        private final String method;

        /**
         * @param _instance currentInstance
         * @throws EFapsException
         */
        public CalcPosition(final Instance _instance)
            throws EFapsException
        {
            super(_instance);

            final PrintQuery print = new PrintQuery(_instance);
            print.addAttribute(CIPayroll.CasePositionCalc.CalculatorESJP,
                            CIPayroll.CasePositionCalc.CalculatorMethod);
            print.execute();
            this.esjp = print.<String>getAttribute(CIPayroll.CasePositionCalc.CalculatorESJP);
            this.method = print.<String>getAttribute(CIPayroll.CasePositionCalc.CalculatorMethod);

            final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.CasePosition2PositionAbstract);
            queryBldr.addWhereAttrEqValue(CIPayroll.CasePosition2PositionAbstract.FromAbstractLink, _instance.getId());
            final MultiPrintQuery multi = queryBldr.getPrint();

            final SelectBuilder oidSel = new SelectBuilder()
                            .linkto(CIPayroll.CasePosition2PositionAbstract.ToAbstractLink)
                            .oid();
            multi.addAttribute(CIPayroll.CasePosition2PositionAbstract.Denominator,
                            CIPayroll.CasePosition2PositionAbstract.Numerator);
            multi.addSelect(oidSel);
            multi.execute();
            while (multi.next()) {
                this.relInst = multi.getCurrentInstance();
                if (this.relInst.isValid()
                                && this.relInst.getType().equals(CIPayroll.CasePositionCalc2Sum4Multiply.getType())) {
                    this.denominator = multi.<Integer>getAttribute(CIPayroll.CasePosition2PositionAbstract.Denominator);
                    this.numerator = multi.<Integer>getAttribute(CIPayroll.CasePosition2PositionAbstract.Numerator);
                    this.toInst = Instance.get(multi.<String>getSelect(oidSel));
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BigDecimal getResult(final Parameter _parameter,
                                    final Map<Instance, Position> _sums,
                                    final Map<Instance, TablePos> _values)
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (_values.containsKey(getInstance())) {
                for (final BigDecimal value : _values.get(getInstance()).getValues()) {
                    ret = ret.add(value);
                }
            }

            if (this.esjp != null && !this.esjp.isEmpty()) {
                try {
                    final Class<?> cls = Class.forName(this.esjp, true, EFapsClassLoader.getInstance());
                    final Method meth = cls.getMethod(this.method, new Class[] { Parameter.class, Map.class, Map.class,
                                                                                    Position.class });
                    ret = (BigDecimal) meth.invoke(cls.newInstance(), _parameter, _sums, _values, this);
                } catch (final SecurityException e) {
                    Payslip_Base.LOG.error("SecurityException", e);
                } catch (final ClassNotFoundException e) {
                    Payslip_Base.LOG.error("ClassNotFoundException", e);
                } catch (final NoSuchMethodException e) {
                    Payslip_Base.LOG.error("NoSuchMethodException", e);
                } catch (final IllegalArgumentException e) {
                    Payslip_Base.LOG.error("IllegalArgumentException", e);
                } catch (final IllegalAccessException e) {
                    Payslip_Base.LOG.error("IllegalAccessException", e);
                } catch (final InvocationTargetException e) {
                    Payslip_Base.LOG.error("InvocationTargetException", e);
                } catch (final InstantiationException e) {
                    Payslip_Base.LOG.error("InvocationTargetException", e);
                }
            } else if (this.toInst != null && this.toInst.isValid()) {
                if (_sums.containsKey(this.toInst)) {
                    final Position sum = _sums.get(this.toInst);
                    final BigDecimal tmp = sum.getResult(_parameter, _sums, _values);
                    ret = tmp.multiply(new BigDecimal(this.numerator).divide(new BigDecimal(this.denominator),
                                    8, BigDecimal.ROUND_HALF_UP));
                    if (_values.containsKey(getInstance())) {
                        _values.get(getInstance()).setSetValue(true);
                        final int count = _values.get(getInstance()).getValues().size();
                        _values.get(getInstance()).getValues().clear();
                        for (int i = 0; i < count; i++) {
                            _values.get(getInstance()).getValues().add(ret);
                        }
                    }
                }
            }
            return ret.setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public class SumPosition
        extends Position
    {

        private final String name;

        private final String description;

        private final Set<Position> children = new HashSet<Position>();

        private final Integer sorted;

        private final Integer mode;

        public SumPosition(final Map<Instance, Position> _sums,
                           final Instance _instance,
                           final String _name,
                           final String _description,
                           final Integer _sorted,
                           final Integer _mode)
            throws EFapsException
        {
            super(_instance);
            this.name = _name;
            this.description = _description;
            this.sorted = _sorted;
            this.mode = _mode;
            final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.CasePositionAbstract);
            queryBldr.addWhereAttrEqValue(CIPayroll.CasePositionAbstract.ParentAbstractLink, getInstance().getId());

            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIPayroll.CasePositionAbstract.Name,
                            CIPayroll.CasePositionAbstract.Description,
                            CIPayroll.CasePositionAbstract.Sorted,
                            CIPayroll.CasePositionAbstract.Mode);
            multi.execute();
            while (multi.next()) {
                final String childName = multi.<String>getAttribute(CIPayroll.CasePositionAbstract.Name);
                final String childDescription = multi.<String>getAttribute(CIPayroll.CasePositionAbstract.Description);
                final Integer sortedTmp = multi.<Integer>getAttribute(CIPayroll.CasePositionAbstract.Sorted);
                final Integer mode = multi.<Integer>getAttribute(CIPayroll.CasePositionAbstract.Mode);
                if (multi.getCurrentInstance().getType().isKindOf(CIPayroll.CasePositionSumAbstract.getType())) {
                    final SumPosition sum = new SumPosition(_sums,
                                    multi.getCurrentInstance(),
                                    childName,
                                    childDescription,
                                    sortedTmp,
                                    mode);
                    _sums.put(multi.getCurrentInstance(), sum);
                    this.children.add(sum);
                } else {
                    final CalcPosition pos = new CalcPosition(multi.getCurrentInstance());
                    _sums.put(multi.getCurrentInstance(), pos);
                    this.children.add(pos);
                }
            }
        }

        /**
         * Getter method for the instance variable {@link #mode}.
         *
         * @return value of instance variable {@link #mode}
         */
        public Integer getMode()
        {
            return this.mode;
        }

        /**
         * Getter method for the instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return this.name;
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
         * Getter method for the instance variable {@link #sorted}.
         *
         * @return value of instance variable {@link #sorted}
         */
        public Integer getSorted()
        {
            return this.sorted;
        }

        /**
         * @param _sums
         * @param _values
         * @return
         */
        @Override
        public BigDecimal getResult(final Parameter _parameter,
                                    final Map<Instance, Position> _sums,
                                    final Map<Instance, TablePos> _values)
        {
            BigDecimal ret = BigDecimal.ZERO;
            for (final Position child : this.children) {
                BigDecimal result = child.getResult(_parameter, _sums, _values);
                if (child.getInstance().getType().equals(CIPayroll.CasePositionDeduction.getType())) {
                    result = result.negate();
                }
                if (!child.getInstance().getType().equals(CIPayroll.CasePositionNeutral.getType())) {
                    ret = ret.add(result);
                }
            }
            return ret.setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
