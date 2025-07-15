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
package org.efaps.esjp.payroll;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field;
import org.efaps.db.AttributeQuery;
import org.efaps.db.CachedPrintQuery;
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
import org.efaps.esjp.ci.CIProjects;
import org.efaps.esjp.ci.CITablePayroll;
import org.efaps.esjp.common.datetime.JodaTimeUtils;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.payroll.basis.BasisAttribute;
import org.efaps.esjp.payroll.listener.IOnPayslip;
import org.efaps.esjp.payroll.rules.AbstractRule;
import org.efaps.esjp.payroll.rules.Calculator;
import org.efaps.esjp.payroll.rules.InputRule;
import org.efaps.esjp.payroll.rules.Result;
import org.efaps.esjp.projects.Project;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.ui.html.Table;
import org.efaps.update.AppDependency;
import org.efaps.update.util.InstallationException;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.builder.style.Styles;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("f1c7d3d5-23d2-4d72-a5d4-3c811a159062")
@EFapsApplication("eFapsApp-Payroll")
public abstract class Payslip_Base
    extends AbstractDocument
{

    /**
     * Key used to store information in the session.
     */
    protected static final String SESSIONKEY = Payslip.class.getName() + ".SessionKey";

    /**
     * Logger for this classes.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Payslip.class);

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
            map.put("eFapsAutoCompleteKEY", oid);
            map.put("eFapsAutoCompleteVALUE", key);
            map.put("eFapsAutoCompleteCHOICE", choice);
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
    public Return deletePreTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink, _parameter.getInstance());
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        while (query.next()) {
            new Delete(query.getCurrentValue()).execute();
        }

        final QueryBuilder queryBldr3 = new QueryBuilder(CIPayroll.Payslip2Advance);
        queryBldr3.addWhereAttrEqValue(CIPayroll.Payslip2Advance.FromLink, _parameter.getInstance());
        final InstanceQuery query3 = queryBldr3.getQuery();
        query3.execute();
        while (query3.next()) {
            new Delete(query3.getCurrentValue()).execute();
        }

        try {
            if (AppDependency.getAppDependency("eFapsApp-Payroll").isMet()) {
                final QueryBuilder queryBldr2 = new QueryBuilder(CIPayroll.Projects_ProjectService2Payslip);
                queryBldr2.addWhereAttrEqValue(CIPayroll.Projects_ProjectService2Payslip.ToDocument,
                                _parameter.getInstance());
                final InstanceQuery query2 = queryBldr2.getQuery();
                query2.execute();
                while (query2.next()) {
                    new Delete(query2.getCurrentValue()).execute();
                }
            }
        } catch (final InstallationException e) {
            throw new EFapsException("Catched Error.", e);
        }
        return new Return();
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
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final CreatedDoc createdDoc = new CreatedDoc();

        final String name = getDocName4Create(_parameter);
        final String docType = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.docType.name);
        final String date = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.date.name);
        final String dueDate = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.dueDate.name);
        final Instance employeeInst = Instance.get(
                        _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.employee.name));
        final String laborTime = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.laborTime.name);
        final String laborTimeUoM = _parameter.getParameterValue("laborTimeUoM");
        final String extraLaborTime = _parameter
                        .getParameterValue(CIFormPayroll.Payroll_PayslipForm.extraLaborTime.name);
        final String extraLaborTimeUoM = _parameter.getParameterValue("extraLaborTimeUoM");
        final String nightLaborTime = _parameter
                        .getParameterValue(CIFormPayroll.Payroll_PayslipForm.nightLaborTime.name);
        final String nightLaborTimeUoM = _parameter.getParameterValue("nightLaborTimeUoM");
        final String holidayLaborTime = _parameter
                        .getParameterValue(CIFormPayroll.Payroll_PayslipForm.holidayLaborTime.name);
        final String holidayLaborTimeUoM = _parameter.getParameterValue("holidayLaborTimeUoM");

        final Instance rateCurrInst = getRateCurrencyInstance(_parameter, createdDoc);

        final Object[] rateObj = getRateObject(_parameter);

        final Instance baseCurInst = Currency.getBaseCurrency();

        final Insert insert = new Insert(CIPayroll.Payslip);
        insert.add(CIPayroll.Payslip.Name, name);
        insert.add(CIPayroll.Payslip.DocType, docType);
        insert.add(CIPayroll.Payslip.Date, date);
        insert.add(CIPayroll.Payslip.DueDate, dueDate);
        insert.add(CIPayroll.Payslip.EmployeeAbstractLink, employeeInst);
        insert.add(CIPayroll.Payslip.StatusAbstract, Status.find(CIPayroll.PayslipStatus.Draft));
        insert.add(CIPayroll.Payslip.RateCrossTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.Payslip.RateNetTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.Payslip.Rate, rateObj);
        insert.add(CIPayroll.Payslip.CrossTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.Payslip.NetTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.Payslip.DiscountTotal, 0);
        insert.add(CIPayroll.Payslip.RateDiscountTotal, 0);
        insert.add(CIPayroll.Payslip.AmountCost, BigDecimal.ZERO);
        insert.add(CIPayroll.Payslip.CurrencyId, baseCurInst);
        insert.add(CIPayroll.Payslip.RateCurrencyId, rateCurrInst);
        insert.add(CIPayroll.Payslip.LaborTime, new Object[] { laborTime, laborTimeUoM });
        insert.add(CIPayroll.Payslip.ExtraLaborTime, new Object[] { extraLaborTime, extraLaborTimeUoM });
        insert.add(CIPayroll.Payslip.HolidayLaborTime, new Object[] { holidayLaborTime, holidayLaborTimeUoM });
        insert.add(CIPayroll.Payslip.NightLaborTime, new Object[] { nightLaborTime, nightLaborTimeUoM });
        insert.add(CIPayroll.Payslip.TemplateLink,  Instance.get(
                        _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.template.name)));
        insert.add(CIPayroll.Payslip.Basis, BasisAttribute.getValueList4Inst(_parameter, employeeInst));
        insert.execute();

        createdDoc.setInstance(insert.getInstance());
        connect2Object(_parameter, createdDoc);

        for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
            listener.afterCreate(_parameter, createdDoc);
        }

        final List<? extends AbstractRule<?>> rules = analyseRulesFomUI(_parameter, getRuleInstFromUI(_parameter));

        final Result result = Calculator.getResult(_parameter, rules);
        updateTotals(_parameter, insert.getInstance(), result, rateCurrInst, rateObj);
        updatePositions(_parameter, insert.getInstance(), result, rateCurrInst, rateObj);

        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }

    /**
     * Create a payslip.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return createMultiple(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> templates = getInstances(_parameter,
                        CIFormPayroll.Payroll_PayslipCreateMultipleForm.templates.name);

        final DateTime date = new DateTime(
                        _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipCreateMultipleForm.date.name));
        final DateTime dueDate = new DateTime(_parameter
                        .getParameterValue(CIFormPayroll.Payroll_PayslipCreateMultipleForm.dueDate.name));

        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Template2EmployeeAbstract);
        queryBldr.addWhereAttrInQuery(CIPayroll.Template2EmployeeAbstract.ToAbstractLink,
                       getAttrQuery4Employees(_parameter, date, dueDate));
        queryBldr.addWhereAttrEqValue(CIPayroll.Template2EmployeeAbstract.FromAbstractLink, templates.toArray());

        final MultiPrintQuery multi = queryBldr.getPrint();

        final SelectBuilder selTemplInst = SelectBuilder.get()
                        .linkto(CIPayroll.Template2EmployeeAbstract.FromAbstractLink).instance();
        final SelectBuilder selEmplInst = SelectBuilder.get()
                        .linkto(CIPayroll.Template2EmployeeAbstract.ToAbstractLink).instance();
        multi.addSelect(selTemplInst, selEmplInst);
        multi.execute();

        final String docType = _parameter.getParameterValue(
                        CIFormPayroll.Payroll_PayslipCreateMultipleForm.docType.name);

        while (multi.next()) {
            final Instance templInst = multi.getSelect(selTemplInst);
            final Instance emplInst = multi.getSelect(selEmplInst);
            create(_parameter, templInst, emplInst, date, dueDate, docType);
        }
        return new Return();
    }

    /**
     * Creates a Payslip.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _templInst the template instance
     * @param _emplInst the employee instance
     * @param _date the date
     * @param _dueDate the due date
     * @param _docType the doc type
     * @throws EFapsException on error
     */
    protected void create(final Parameter _parameter,
                          final Instance _templInst,
                          final Instance _emplInst,
                          final DateTime _date,
                          final DateTime _dueDate,
                          final Object _docType)
        throws EFapsException
    {
        final Object[] rateObj = new Object[] { BigDecimal.ONE, BigDecimal.ONE };
        final Dimension timeDim = CIPayroll.Payslip.getType().getAttribute(CIPayroll.Payslip.LaborTime.name)
                        .getDimension();
        final List<Instance> timecardInsts = new ArrayList<>();
        for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
            timecardInsts.addAll(listener.getEmployeeTimeCardInst(_parameter, _emplInst));
        }
        final List<String> timecardOids = new ArrayList<>();
        for (final Instance timecardInst : timecardInsts) {
            timecardOids.add(timecardInst.getOid());
        }
        if (!timecardOids.isEmpty()) {
            ParameterUtil.setParameterValues(_parameter, "TimeReport_EmployeeTimeCard",
                            timecardOids.toArray(new String[timecardOids.size()]));
        }
        final CreatedDoc createdDoc = new CreatedDoc();

        final Insert insert = new Insert(CIPayroll.Payslip);
        insert.add(CIPayroll.Payslip.Name, getDocName4Create(_parameter));
        insert.add(CIPayroll.Payslip.DocType, _docType);
        insert.add(CIPayroll.Payslip.Date, getStartDate(_parameter, _date, _emplInst));
        insert.add(CIPayroll.Payslip.DueDate, getEndDate(_parameter, _date, _dueDate, _emplInst));
        insert.add(CIPayroll.Payslip.EmployeeAbstractLink, _emplInst);
        insert.add(CIPayroll.Payslip.StatusAbstract, Status.find(CIPayroll.PayslipStatus.Draft));
        insert.add(CIPayroll.Payslip.RateCrossTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.Payslip.RateNetTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.Payslip.Rate, rateObj);
        insert.add(CIPayroll.Payslip.CrossTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.Payslip.NetTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.Payslip.DiscountTotal, 0);
        insert.add(CIPayroll.Payslip.RateDiscountTotal, 0);
        insert.add(CIPayroll.Payslip.AmountCost, BigDecimal.ZERO);
        insert.add(CIPayroll.Payslip.CurrencyId, Currency.getBaseCurrency());
        insert.add(CIPayroll.Payslip.RateCurrencyId, Currency.getBaseCurrency());
        insert.add(CIPayroll.Payslip.LaborTime, new Object[] {
                        getLaborTime(_parameter, null, _date, _dueDate, _emplInst, _templInst),
                        timeDim.getBaseUoM().getId() });
        insert.add(CIPayroll.Payslip.ExtraLaborTime,
                        new Object[] {  getExtraLaborTime(_parameter, null, _date, _dueDate, _emplInst, _templInst),
                                        timeDim.getBaseUoM().getId() });
        insert.add(CIPayroll.Payslip.HolidayLaborTime, new Object[] {
                        getHolidayLaborTime(_parameter, null, _date, _dueDate, _emplInst, _templInst),
                        timeDim.getBaseUoM().getId() });
        insert.add(CIPayroll.Payslip.NightLaborTime,
                        new Object[] {  getNightLaborTime(_parameter, null, _date, _dueDate, _emplInst, _templInst),
                                        timeDim.getBaseUoM().getId() });
        insert.add(CIPayroll.Payslip.TemplateLink, _templInst);
        insert.add(CIPayroll.Payslip.Basis, BasisAttribute.getValueList4Inst(_parameter, _emplInst));

        if (_parameter.getInstance() != null
                        && _parameter.getInstance().getType().isKindOf(CIPayroll.ProcessAbstract)) {
            insert.add(CIPayroll.Payslip.ProcessAbstractLink, _parameter.getInstance());
        }
        insert.execute();

        createdDoc.setInstance(insert.getInstance());
        addAdvance(_parameter, createdDoc, _emplInst);
        connect2Project(_parameter, createdDoc, _emplInst);

        for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
            listener.afterCreate(_parameter, createdDoc);
        }

        final List<? extends AbstractRule<?>> rules = Template.getRules4Template(_parameter, _templInst);
        Calculator.evaluate(_parameter, rules, createdDoc.getInstance());
        final Result result = Calculator.getResult(_parameter, rules);
        updateTotals(_parameter, insert.getInstance(), result, Currency.getBaseCurrency(), rateObj);
        updatePositions(_parameter, insert.getInstance(), result, Currency.getBaseCurrency(), rateObj);

        createReport(_parameter, createdDoc);
    }

    @Override
    protected Status getStatus4evaluateTimes(final Parameter _parameter)
        throws EFapsException
    {
        return Status.find(CIPayroll.PayslipStatus.Draft);
    }





    /**
     * Adds the advance.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _createdDoc the created doc
     * @param _emplInst the empl inst
     * @throws EFapsException on error
     */
    protected void addAdvance(final Parameter _parameter,
                              final CreatedDoc _createdDoc,
                              final Instance _emplInst)
        throws EFapsException
    {
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip2Advance);
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Advance);
        queryBldr.addWhereAttrNotInQuery(CIPayroll.Advance.ID,
                        attrQueryBldr.getAttributeQuery(CIPayroll.Payslip2Advance.ToLink));
        queryBldr.addWhereAttrEqValue(CIPayroll.Advance.EmployeeAbstractLink, _emplInst);
        queryBldr.addWhereAttrEqValue(CIPayroll.Advance.Status, Status.find(CIPayroll.AdvanceStatus.Paid));
        final InstanceQuery query = queryBldr.getQuery();
        for (final Instance instance : query.execute()) {
            final Insert insert = new Insert(CIPayroll.Payslip2Advance);
            insert.add(CIPayroll.Payslip2Advance.FromLink, _createdDoc.getInstance());
            insert.add(CIPayroll.Payslip2Advance.ToLink, instance);
            insert.execute();
        }
    }

    /**
     * Connect2 project.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _createdDoc the created doc
     * @param _emplInst the empl inst
     * @throws EFapsException on error
     */
    protected void connect2Project(final Parameter _parameter,
                                   final CreatedDoc _createdDoc,
                                   final Instance _emplInst)
        throws EFapsException
    {
        try {
            if (AppDependency.getAppDependency("eFapsApp-Payroll").isMet()) {
                final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProjectService2Employee);
                queryBldr.addWhereAttrEqValue(CIProjects.ProjectService2Employee.ToLink, _emplInst);
                queryBldr.addWhereAttrEqValue(CIProjects.ProjectService2Employee.Status,
                                Status.find(CIProjects.ProjectService2EmployeeStatus.Active));
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selProj = SelectBuilder.get().linkto(
                                CIProjects.ProjectService2Employee.FromLink);
                final SelectBuilder selProjInst = new SelectBuilder(selProj).instance();
                multi.addSelect(selProjInst);
                multi.execute();
                if (multi.next()) {
                    final Instance projInst = multi.getSelect(selProjInst);
                    final Insert insert = new Insert(CIPayroll.Projects_ProjectService2Payslip);
                    insert.add(CIPayroll.Projects_ProjectService2Payslip.FromLink, projInst);
                    insert.add(CIPayroll.Projects_ProjectService2Payslip.ToLink, _createdDoc.getInstance());
                    insert.execute();
                }
            }
        } catch (final InstallationException e) {
            throw new EFapsException("Catched Error.", e);
        }
    }

    /**
     * Edits the.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final Instance instance = _parameter.getInstance();
        final String docType = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.docType.name);
        final String date = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.date.name);
        final String dueDate = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.dueDate.name);
        final String laborTime = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.laborTime.name);
        final String laborTimeUoM = _parameter.getParameterValue("laborTimeUoM");
        final String extraLaborTime = _parameter
                        .getParameterValue(CIFormPayroll.Payroll_PayslipForm.extraLaborTime.name);
        final String extraLaborTimeUoM = _parameter.getParameterValue("extraLaborTimeUoM");
        final String nightLaborTime = _parameter
                        .getParameterValue(CIFormPayroll.Payroll_PayslipForm.nightLaborTime.name);
        final String nightLaborTimeUoM = _parameter.getParameterValue("nightLaborTimeUoM");
        final String holidayLaborTime = _parameter
                        .getParameterValue(CIFormPayroll.Payroll_PayslipForm.holidayLaborTime.name);
        final String holidayLaborTimeUoM = _parameter.getParameterValue("holidayLaborTimeUoM");

        final Update update = new Update(instance);
        update.add(CIPayroll.Payslip.DocType, docType);
        update.add(CIPayroll.Payslip.LaborTime, new Object[] { laborTime, laborTimeUoM });
        update.add(CIPayroll.Payslip.ExtraLaborTime, new Object[] { extraLaborTime, extraLaborTimeUoM });
        update.add(CIPayroll.Payslip.NightLaborTime, new Object[] { nightLaborTime, nightLaborTimeUoM });
        update.add(CIPayroll.Payslip.HolidayLaborTime, new Object[] { holidayLaborTime, holidayLaborTimeUoM });
        update.add(CIPayroll.Payslip.Date, date);
        update.add(CIPayroll.Payslip.DueDate, dueDate);
        update.add(CIPayroll.Payslip.Basis, BasisAttribute.getValueList4Inst(_parameter, instance));
        update.execute();

        final PrintQuery print = new PrintQuery(instance);
        final SelectBuilder selRateCurInst = SelectBuilder.get().linkto(CIPayroll.Payslip.RateCurrencyId).instance();
        print.addSelect(selRateCurInst);
        print.addAttribute(CIPayroll.Payslip.Rate);
        print.execute();

        final Instance rateCurrInst = print.getSelect(selRateCurInst);
        final Object[] rateObj = print.getAttribute(CIPayroll.Payslip.Rate);

        final List<? extends AbstractRule<?>> rules = analyseRulesFomUI(_parameter, getRuleInstFromUI(_parameter));
        final Result result = Calculator.getResult(_parameter, rules);
        updateTotals(_parameter, instance, result, rateCurrInst, rateObj);
        updatePositions(_parameter, instance, result, rateCurrInst, rateObj);

        final EditedDoc editedDoc = new EditedDoc(instance);

        final File file = createReport(_parameter, editedDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }



    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return positionMultiPrint(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, Collections.<Instance>emptyList());
        return ret;
    }

    /**
     * Create massive payslips.
     *
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
        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
        try {
            final Instance baseCurIns = Currency.getBaseCurrency();
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
     * @param _parameter    Paramter as passed by the eFaps API
     * @return script for setting the positions
     * @throws EFapsException on error
     */
    public Return getJavaScript4EditUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        final List<? extends AbstractRule<?>> rules = analyseRulesFomDoc(_parameter, _parameter.getInstance());

        final List<Map<String, Object>> strValues = new ArrayList<>();

        for (final AbstractRule<?> rule : rules) {
            final Map<String, Object> map = new HashMap<>();
            map.put("rulePosition", new String[] { rule.getInstance().getOid(), rule.getKey() });
            map.put("ruleDescription", rule.getDescription());
            map.put("ruleAmount", rule.getResult());
            strValues.add(map);
        }

        js.append(getTableRemoveScript(_parameter, "ruleTable", false, false))
                        .append(getTableAddNewRowsScript(_parameter, "ruleTable", strValues, new StringBuilder()));
        final Return ret = new Return();
        ret.put(ReturnValues.SNIPLETT, InterfaceUtils.wrappInScriptTag(_parameter, js, true, 1500));
        return ret;
    }

    /**
     * Method to get the javascript part for setting the values.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _instance instance to be copied
     * @return javascript
     * @throws EFapsException on error
     */
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

        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();

        js.append("function setValue() {\n")
                        .append("document.getElementsByName('")
                        .append(CIFormPayroll.Payroll_PayslipForm.rateCurrencyId.name)
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
     * Method for return a first date of month and last date of month.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return ret Return.
     * @throws EFapsException on error.
     */
    public Return dateValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final IUIValue fValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final DateTime value;
        if (TargetMode.CREATE.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            if (fValue.getField().getName().equals("date")) {
                value = new DateTime().dayOfMonth().withMinimumValue();
            } else {
                value = new DateTime().dayOfMonth().withMaximumValue();
            }
        } else {
            value = (DateTime) fValue.getObject();
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
    public Return updateFields4Template(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();

        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);

        if (field != null) {
            final Instance templInst = Instance.get(_parameter.getParameterValue(field.getName()));
            if (templInst.isValid()) {
                final DecimalFormat formatter = NumberFormatter.get().getFormatter();
                final BigDecimal laborTime = getDefaultValue(_parameter, templInst,
                                CIPayroll.TemplatePayslip.DefaultLaborTime);
                if (laborTime != null) {
                    map.put("laborTime", formatter.format(laborTime));
                }
                final BigDecimal extraLaborTime = getDefaultValue(_parameter, templInst,
                                CIPayroll.TemplatePayslip.DefaultExtraLaborTime);
                if (extraLaborTime != null) {
                    map.put("extraLaborTime", formatter.format(extraLaborTime));
                }
                final BigDecimal nightLaborTime = getDefaultValue(_parameter, templInst,
                                CIPayroll.TemplatePayslip.DefaultNightLaborTime);
                if (nightLaborTime != null) {
                    map.put("nightLaborTime", formatter.format(nightLaborTime));
                }
                final BigDecimal holidayLaborTime = getDefaultValue(_parameter, templInst,
                                CIPayroll.TemplatePayslip.DefaultHolidayLaborTime);
                if (holidayLaborTime != null) {
                    map.put("holidayLaborTime", formatter.format(holidayLaborTime));
                }
            }
            list.add(map);
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Update fields4 employee.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException on error
     */
    public Return updateFields4Employee(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();

        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);

        if (field != null) {
            final Instance emplInst = Instance.get(_parameter.getParameterValue(field.getName()));
            if (emplInst.isValid()) {
                map.put("employeeData", getFieldValue4Employee(emplInst));
                try {
                    if (AppDependency.getAppDependency("eFapsApp-Payroll").isMet()) {
                        final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProjectService2Employee);
                        queryBldr.addWhereAttrEqValue(CIProjects.ProjectService2Employee.ToLink, emplInst);
                        queryBldr.addWhereAttrEqValue(CIProjects.ProjectService2Employee.Status,
                                        Status.find(CIProjects.ProjectService2EmployeeStatus.Active));
                        final MultiPrintQuery multi = queryBldr.getPrint();
                        final SelectBuilder selProj = SelectBuilder.get().linkto(
                                        CIProjects.ProjectService2Employee.FromLink);
                        final SelectBuilder selProjInst = new SelectBuilder(selProj).instance();
                        final SelectBuilder selProjName = new SelectBuilder(selProj).attribute("Name");
                        multi.addSelect(selProjInst, selProjName);
                        multi.execute();
                        while (multi.next()) {
                            map.put("project", new String[] { multi.<Instance>getSelect(selProjInst).getOid(),
                                            multi.<String>getSelect(selProjName) });
                            map.put("projectData", new Project().getProjectData(_parameter,
                                            multi.<Instance>getSelect(selProjInst)).toString());
                        }
                    }
                } catch (final InstallationException e) {
                    throw new EFapsException("Catched Error.", e);
                }
                for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
                    listener.add2UpdateMap4Employee(_parameter, emplInst, map);
                }
                final DateTime date = JodaTimeUtils.getDateFromParameter(_parameter.getParameterValue("date_eFapsDate"));
                map.put("date_eFapsDate", getStartDate(_parameter, date, emplInst));
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
     * Gets the start date.
     *
     * @param _parameter the _parameter
     * @param _date the _date
     * @param _emplInst the _empl inst
     * @return the date
     * @throws EFapsException on error
     */
    protected DateTime getStartDate(final Parameter _parameter,
                                    final DateTime _date,
                                    final Instance _emplInst)
        throws EFapsException
    {
        DateTime ret = _date;
        final PrintQuery print = CachedPrintQuery.get4Request(_emplInst);
        final SelectBuilder selStartDate = SelectBuilder.get().clazz(CIHumanResource.ClassTR)
                        .attribute(CIHumanResource.ClassTR.StartDate);
        print.addSelect(selStartDate);
        print.execute();
        final DateTime startDate = print.getSelect(selStartDate);
        if (startDate != null && ret.isBefore(startDate)) {
            ret = startDate;
        }
        return ret;
    }

    /**
     * Gets the start date.
     *
     * @param _parameter the _parameter
     * @param _startDate the start date
     * @param _endDate the end date
     * @param _emplInst the _empl inst
     * @return the date
     * @throws EFapsException on error
     */
    protected DateTime getEndDate(final Parameter _parameter,
                                  final DateTime _startDate,
                                  final DateTime _endDate,
                                  final Instance _emplInst)
        throws EFapsException
    {
        DateTime ret = _endDate;
        final PrintQuery print = CachedPrintQuery.get4Request(_emplInst);
        final SelectBuilder selEndDate = SelectBuilder.get().clazz(CIHumanResource.ClassTR)
                        .attribute(CIHumanResource.ClassTR.EndDate);
        print.addSelect(selEndDate);
        print.execute();
        final DateTime endDate = print.getSelect(selEndDate);
        if (endDate != null && endDate.isBefore(ret) && endDate.isAfter(_startDate)) {
            ret = endDate;
        }
        return ret;
    }

    /**
     * Executed the command on the button.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return executeButton(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder js = new StringBuilder();
        final Instance templInst = Instance.get(_parameter
                        .getParameterValue(CIFormPayroll.Payroll_PayslipForm.template.name));
        if (templInst.isValid()) {
            final List<? extends AbstractRule<?>> rules = Template.getRules4Template(_parameter, templInst);

            final Collection<Map<String, Object>> values = new ArrayList<>();
            for (final AbstractRule<?> rule : rules) {
                final Map<String, Object> map = new HashMap<>();
                values.add(map);
                map.put(CITablePayroll.Payroll_PositionRuleTable.rulePosition.name,
                                new String[] { rule.getInstance().getOid(), rule.getKey() });
                map.put(CITablePayroll.Payroll_PositionRuleTable.ruleDescription.name, rule.getDescription());
            }
            js.append(getTableRemoveScript(_parameter, "ruleTable"))
                            .append(getTableAddNewRowsScript(_parameter, "ruleTable", values, null));

        }
        final Instance emplInst = Instance.get(_parameter
                        .getParameterValue(CIFormPayroll.Payroll_PayslipForm.employee.name));
        if (emplInst.isValid()) {
            final DateTime date = DateUtil.getDateFromParameter(_parameter.getParameterValue(
                            CIFormPayroll.Payroll_PayslipForm.date.name + "_eFapsDate"));
            final DateTime dueDate = DateUtil.getDateFromParameter(_parameter.getParameterValue(
                            CIFormPayroll.Payroll_PayslipCreateMultipleForm.dueDate.name + "_eFapsDate"));
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();

            final String laborTime = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.laborTime.name);
            if (laborTime == null || laborTime != null && laborTime.isEmpty()) {
                final BigDecimal time = getLaborTime(_parameter, null, date, dueDate, emplInst, templInst);
                if (time != null) {
                    js.append(getSetFieldValue(0, CIFormPayroll.Payroll_PayslipForm.laborTime.name,
                                    formatter.format(time)));
                }
            }
            final String extraLaborTime = _parameter
                            .getParameterValue(CIFormPayroll.Payroll_PayslipForm.extraLaborTime.name);
            if (extraLaborTime == null || extraLaborTime != null && extraLaborTime.isEmpty()) {
                final BigDecimal time = getExtraLaborTime(_parameter, null, date, dueDate, emplInst, templInst);
                if (time != null) {
                    js.append(getSetFieldValue(0, CIFormPayroll.Payroll_PayslipForm.extraLaborTime.name,
                                    formatter.format(time)));
                }
            }
            final String nightLaborTime = _parameter
                            .getParameterValue(CIFormPayroll.Payroll_PayslipForm.nightLaborTime.name);
            if (nightLaborTime == null || nightLaborTime != null && nightLaborTime.isEmpty()) {
                final BigDecimal time = getNightLaborTime(_parameter, null, date, dueDate, emplInst, templInst);
                if (time != null) {
                    js.append(getSetFieldValue(0, CIFormPayroll.Payroll_PayslipForm.nightLaborTime.name,
                                    formatter.format(time)));
                }
            }
            final String holidayLaborTime = _parameter
                            .getParameterValue(CIFormPayroll.Payroll_PayslipForm.holidayLaborTime.name);
            if (holidayLaborTime == null || holidayLaborTime != null && holidayLaborTime.isEmpty()) {
                final BigDecimal time = getHolidayLaborTime(_parameter, null, date, dueDate, emplInst, templInst);
                if (time != null) {
                    js.append(getSetFieldValue(0, CIFormPayroll.Payroll_PayslipForm.holidayLaborTime.name,
                                    formatter.format(time)));
                }
            }
        }
        if (js.length() > 0) {
            ret.put(ReturnValues.SNIPLETT, js.toString());
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
                        .append(StringEscapeUtils.escapeEcmaScript(html)).append("';")
                        .append("positionTableColumns(eFapsTable100);");
        map.put("eFapsFieldUpdateJS", js.toString());
        list.add(map);

        for (final AbstractRule<?> rule : rules) {
            final Map<String, Object> map2 = new HashMap<>();
            list.add(map2);
            map2.put("eFapsFieldUseIndex", ruleInsts.get(rule.getInstance()));
            map2.put(CITablePayroll.Payroll_PositionRuleTable.ruleAmount.name, rule.getResult());
        }

        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * Analyse rules fom ui.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _ruleInsts the rule insts
     * @return the list<? extends abstract rule<?>>
     * @throws EFapsException on error
     */
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
                final String amountStr = amounts[_ruleInsts.get(rule.getInstance())];
                try {
                    final BigDecimal amount;
                    if (amountStr.isEmpty()) {
                        amount = BigDecimal.ZERO;
                    } else {
                        amount = (BigDecimal) NumberFormatter.get().getTwoDigitsFormatter()
                                        .parse(amountStr);
                    }
                    rule.setExpression(Calculator.toJexlBigDecimal(_parameter, amount));
                } catch (final ParseException e) {
                    LOG.error("Catched ParserException", e);
                }
            }
        }
        Calculator.evaluate(_parameter, ret);
        return ret;
    }

    /**
     * Gets the html4 positions.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _rules the rules
     * @return the html4 positions
     * @throws EFapsException on error
     */
    protected CharSequence getHtml4Positions(final Parameter _parameter,
                                             final List<? extends AbstractRule<?>> _rules)
        throws EFapsException
    {
        final Result result = Calculator.getResult(_parameter, _rules);
        final Table table = new Table();
        table.addRow().addColumn(CIPayroll.PositionPayment.getType().getLabel())
                        .getCurrentColumn().setStyle("font-weight: bold;");
        for (final AbstractRule<?> rule : result.getPaymentRules()) {
            if (rule.add()) {
                table.addRow().addColumn(rule.getKey()).addColumn(rule.getDescription())
                    .addColumn(String.valueOf(rule.getResult()))
                    .getCurrentColumn().setStyle("text-align: right;");
            }
        }
        table.addRow().addColumn("").getCurrentColumn().setColSpan(2).getRow()
            .addColumn(result.getPaymentFrmt()).getCurrentColumn().setStyle("text-align: right;");

        table.addRow().addColumn(CIPayroll.PositionDeduction.getType().getLabel())
                        .getCurrentColumn().setStyle("font-weight: bold;");
        for (final AbstractRule<?> rule : result.getDeductionRules()) {
            if (rule.add()) {
                table.addRow().addColumn(rule.getKey()).addColumn(rule.getDescription())
                            .addColumn(String.valueOf(rule.getResult()))
                            .getCurrentColumn().setStyle("text-align: right;");
            }
        }
        table.addRow().addColumn("").getCurrentColumn().setColSpan(2).getRow()
            .addColumn(result.getDeductionFrmt()).getCurrentColumn().setStyle("text-align: right;");

        table.addRow().addColumn(CIPayroll.PositionNeutral.getType().getLabel())
                        .getCurrentColumn().setStyle("font-weight: bold;");
        for (final AbstractRule<?> rule : result.getNeutralRules()) {
            if (rule.add()) {
                table.addRow().addColumn(rule.getKey()).addColumn(rule.getDescription())
                            .addColumn(String.valueOf(rule.getResult()))
                            .getCurrentColumn().setStyle("text-align: right;");
            }
        }
        table.addRow().addColumn("").getCurrentColumn().setColSpan(2).getRow().addColumn(result.getNeutralFrmt())
            .getCurrentColumn().setStyle("text-align: right;");

        table.addRow().addColumn("").getCurrentColumn().setColSpan(2).getRow()
            .addColumn(result.getTotalFrmt()).getCurrentColumn().setStyle("text-align: right;font-weight: bold;");
        return table.toHtml();
    }

    /**
     * Gets the rule inst from ui.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the rule inst from ui
     */
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
     * Method to create a report for the accounting actions definition, related
     * with the payslip cases.
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
                                                DynamicReports.cmp
                                                        .text(new Date())
                                                        .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)
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
                                                    .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)));
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

    /**
     * Gets the action report data source.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the action report data source
     * @throws EFapsException on error
     */
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



    /**
     * Gets the attribute query 4 employees.
     * All active employees and inactive in the given timeframe.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _minDate the min date
     * @param _maxDate the max date
     * @return the AttributeQuery for employees
     * @throws EFapsException on error
     */
    protected AttributeQuery getAttrQuery4Employees(final Parameter _parameter,
                                                    final DateTime _minDate,
                                                    final DateTime _maxDate)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIHumanResource.Employee);
        queryBldr.addWhereAttrEqValue(CIHumanResource.Employee.Status, Status.find(
                        CIHumanResource.EmployeeStatus.Worker));

        if (_minDate != null && _maxDate != null) {
            // inactive employees that where inactivated in the given timeframe
            final QueryBuilder classQueryBldr = new QueryBuilder(CIHumanResource.ClassTR);
            classQueryBldr.addWhereAttrGreaterValue(CIHumanResource.ClassTR.EndDate,
                            _minDate.withTimeAtStartOfDay().minusDays(1));
            classQueryBldr.addWhereAttrLessValue(CIHumanResource.ClassTR.EndDate,
                            _maxDate.withTimeAtStartOfDay().plusDays(1));

            final QueryBuilder queryBldr2 = new QueryBuilder(CIHumanResource.Employee);
            queryBldr2.addWhereAttrEqValue(CIHumanResource.Employee.Status, Status.find(
                            CIHumanResource.EmployeeStatus.Notworked));
            queryBldr2.addWhereAttrInQuery(CIHumanResource.Employee.ID, classQueryBldr.getAttributeQuery(
                            CIHumanResource.ClassTR.EmployeeLink));
            queryBldr.setOr(true);
            queryBldr.addWhereAttrInQuery(CIHumanResource.Employee.ID,
                            queryBldr2.getAttributeQuery(CIHumanResource.Employee.ID));
        }
        return queryBldr.getAttributeQuery(CIHumanResource.Employee.ID);
    }

    /**
     * Warning for existing name.
     *
     * @author The eFaps Team
     */
    public static class PayslipNoSelection4EditMassiveWarning
        extends AbstractWarning
    {

        /**
         * Instantiates a new payslip no selection4 edit massive warning.
         */
        public PayslipNoSelection4EditMassiveWarning()
        {
            setError(true);
        }
    }

    /**
     * Warning for existing name.
     *
     * @author The eFaps Team
     */
    public static class PayslipSelectionQuantity4EditMassiveWarning
        extends AbstractWarning
    {

        /**
         * Instantiates a new payslip selection quantity4 edit massive warning.
         */
        public PayslipSelectionQuantity4EditMassiveWarning()
        {
            setError(true);
        }
    }
}
