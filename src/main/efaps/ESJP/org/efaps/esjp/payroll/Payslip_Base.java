/*
 * Copyright 2003 - 2015 The eFaps Team
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field;
import org.efaps.ci.CIAttribute;
import org.efaps.ci.CIType;
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
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.common.util.InterfaceUtils_Base.DojoLibs;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.payroll.listener.IOnPayslip;
import org.efaps.esjp.payroll.rules.AbstractRule;
import org.efaps.esjp.payroll.rules.Calculator;
import org.efaps.esjp.payroll.rules.IDocRuleListener;
import org.efaps.esjp.payroll.rules.InputRule;
import org.efaps.esjp.payroll.rules.Result;
import org.efaps.esjp.payroll.util.Payroll;
import org.efaps.esjp.projects.Project;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.document.AbstractDocumentSum;
import org.efaps.esjp.ui.html.Table;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.ui.wicket.util.EFapsKey;
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
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
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
    extends AbstractDocumentSum
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
            if (AppDependency.getAppDependency("eFapsApp-Projects").isMet()) {
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

        final String date = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.date.name);
        final String dueDate = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.dueDate.name);
        final Long employeeid = Instance.get(
                        _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.employee.name)).getId();
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
        insert.add(CIPayroll.Payslip.Date, date);
        insert.add(CIPayroll.Payslip.DueDate, dueDate);
        insert.add(CIPayroll.Payslip.EmployeeAbstractLink, employeeid);
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
        final DateTime date = new DateTime(
                        _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipCreateMultipleForm.date.name));
        final DateTime dueDate = new DateTime(_parameter
                        .getParameterValue(CIFormPayroll.Payroll_PayslipCreateMultipleForm.dueDate.name));

        final Dimension timeDim = CIPayroll.Payslip.getType().getAttribute(CIPayroll.Payslip.LaborTime.name)
                        .getDimension();
        while (multi.next()) {
            final Instance templInst = multi.getSelect(selTemplInst);
            final Instance emplInst = multi.getSelect(selEmplInst);
            final List<Instance> timecardInsts = new ArrayList<>();
            for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
                timecardInsts.addAll(listener.getEmployeeTimeCardInst(_parameter, emplInst));
            }
            final List<String> timecardOids = new ArrayList<>();
            for (final Instance timecardInst : timecardInsts) {
                timecardOids.add(timecardInst.getOid());
            }
            if (!timecardOids.isEmpty()) {
                ParameterUtil.setParmeterValue(_parameter, "TimeReport_EmployeeTimeCard",
                                timecardOids.toArray(new String[timecardOids.size()]));
            }
            final CreatedDoc createdDoc = new CreatedDoc();

            final Insert insert = new Insert(CIPayroll.Payslip);
            insert.add(CIPayroll.Payslip.Name, getDocName4Create(_parameter));
            insert.add(CIPayroll.Payslip.Date, date);
            insert.add(CIPayroll.Payslip.DueDate, dueDate);
            insert.add(CIPayroll.Payslip.EmployeeAbstractLink, emplInst);
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
                            getLaborTime(_parameter, null, date, dueDate, emplInst, templInst),
                            timeDim.getBaseUoM().getId() });
            insert.add(CIPayroll.Payslip.ExtraLaborTime,
                            new Object[] {  getExtraLaborTime(_parameter, null, date, dueDate, emplInst, templInst),
                                            timeDim.getBaseUoM().getId() });
            insert.add(CIPayroll.Payslip.HolidayLaborTime, new Object[] {
                            getHolidayLaborTime(_parameter, null, date, dueDate, emplInst, templInst),
                            timeDim.getBaseUoM().getId() });
            insert.add(CIPayroll.Payslip.NightLaborTime,
                            new Object[] {  getNightLaborTime(_parameter, null, date, dueDate, emplInst, templInst),
                                            timeDim.getBaseUoM().getId() });
            insert.add(CIPayroll.Payslip.TemplateLink, templInst);
            insert.execute();

            createdDoc.setInstance(insert.getInstance());
            addAdvance(_parameter, createdDoc, emplInst);
            connect2Project(_parameter, createdDoc, emplInst);

            for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
                listener.afterCreate(_parameter, createdDoc);
            }

            final List<? extends AbstractRule<?>> rules = Template.getRules4Template(_parameter, templInst);
            Calculator.evaluate(_parameter, rules, createdDoc.getInstance());
            final Result result = Calculator.getResult(_parameter, rules);
            updateTotals(_parameter, insert.getInstance(), result, Currency.getBaseCurrency(), rateObj);
            updatePositions(_parameter, insert.getInstance(), result, Currency.getBaseCurrency(), rateObj);

            createReport(_parameter, createdDoc);
        }
        return new Return();
    }

    public Return evaluateTimes(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> payslipInsts = new ArrayList<>();

        final Instance tmpinst = _parameter.getInstance();
        if (tmpinst != null && tmpinst.isValid()) {
            payslipInsts.add(tmpinst);
        } else {
            payslipInsts.addAll(getSelectedInstances(_parameter));
        }

        for (final Instance payslipInst : payslipInsts) {
            final PrintQuery print = new PrintQuery(payslipInst);
            final SelectBuilder selEmplInst = SelectBuilder.get().linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                            .instance();
            final SelectBuilder selTemplInst = SelectBuilder.get().linkto(CIPayroll.Payslip.TemplateLink)
                            .instance();
            print.addSelect(selEmplInst, selTemplInst);
            print.addAttribute(CIPayroll.Payslip.Date, CIPayroll.Payslip.DueDate, CIPayroll.Payslip.Status);
            print.execute();
            final Status status = Status.get(print.<Long>getAttribute(CIPayroll.Payslip.Status));
            if (Status.find(CIPayroll.PayslipStatus.Draft).equals(status)) {
                final DateTime date = print.getAttribute(CIPayroll.Payslip.Date);
                final DateTime dueDate = print.getAttribute(CIPayroll.Payslip.DueDate);
                final Instance emplInst = print.getSelect(selEmplInst);
                final Instance templInst = print.getSelect(selTemplInst);

                final Dimension timeDim = CIPayroll.Payslip.getType().getAttribute(CIPayroll.Payslip.LaborTime.name)
                                .getDimension();

                final BigDecimal lT = getLaborTime(_parameter, payslipInst, date, dueDate, emplInst, templInst);
                final BigDecimal elT = getExtraLaborTime(_parameter, payslipInst, date, dueDate, emplInst, templInst);
                final BigDecimal nlT = getNightLaborTime(_parameter, payslipInst, date, dueDate, emplInst, templInst);
                final BigDecimal hlT = getHolidayLaborTime(_parameter, payslipInst, date, dueDate, emplInst, templInst);

                if (lT != null || elT != null || nlT != null || hlT != null) {
                    final Update update = new Update(payslipInst);
                    if (lT != null) {
                        update.add(CIPayroll.Payslip.LaborTime, new Object[] { lT, timeDim.getBaseUoM().getId() });
                    }
                    if (elT != null) {
                        update.add(CIPayroll.Payslip.ExtraLaborTime, new Object[] { elT, timeDim.getBaseUoM().getId() });
                    }
                    if (nlT != null) {
                        update.add(CIPayroll.Payslip.NightLaborTime, new Object[] { nlT, timeDim.getBaseUoM().getId() });
                    }
                    if (hlT != null) {
                        update.add(CIPayroll.Payslip.HolidayLaborTime,
                                        new Object[] { hlT, timeDim.getBaseUoM().getId() });
                    }
                    update.execute();
                }
            }
        }
        return new Return();
    }

    protected BigDecimal getLaborTime(final Parameter _parameter,
                                      final Instance _payslipInst,
                                      final DateTime _date,
                                      final DateTime _dueDate,
                                      final Instance _emplInst,
                                      final Instance _templInst)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (Payroll.PAYSLIPEVALLABORTIME.get()) {
            for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
                ret = listener.getLaborTime(_parameter, _payslipInst, _date, _dueDate, _emplInst);
            }
        }
        if (ret == null) {
            ret = getDefaultValue(_parameter, _templInst, CIPayroll.TemplatePayslip.DefaultLaborTime);
        }
        return ret;
    }

    protected BigDecimal getExtraLaborTime(final Parameter _parameter,
                                           final Instance _payslipInst,
                                           final DateTime _date,
                                           final DateTime _dueDate,
                                           final Instance _emplInst,
                                           final Instance _templInst)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (Payroll.PAYSLIPEVALEXTRALABORTIME.get()) {
            for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
                ret = listener.getExtraLaborTime(_parameter, _payslipInst, _date, _dueDate, _emplInst);
            }
        }
        if (ret == null) {
            ret = getDefaultValue(_parameter, _templInst, CIPayroll.TemplatePayslip.DefaultExtraLaborTime);
        }
        return ret;
    }

    protected BigDecimal getNightLaborTime(final Parameter _parameter,
                                           final Instance _payslipInst,
                                           final DateTime _date,
                                           final DateTime _dueDate,
                                           final Instance _emplInst,
                                           final Instance _templInst)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (Payroll.PAYSLIPEVALNIGHTLABORTIME.get()) {
            for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
                ret = listener.getNightLaborTime(_parameter, _payslipInst, _date, _dueDate, _emplInst);
            }
        }
        if (ret == null) {
            ret = getDefaultValue(_parameter, _templInst, CIPayroll.TemplatePayslip.DefaultNightLaborTime);
        }
        return ret;
    }

    protected BigDecimal getHolidayLaborTime(final Parameter _parameter,
                                             final Instance _payslipInst,
                                             final DateTime _date,
                                             final DateTime _dueDate,
                                             final Instance _emplInst,
                                             final Instance _templInst)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (Payroll.PAYSLIPEVALHOLIDAYLABORTIME.get()) {
            for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
                ret = listener.getHolidayLaborTime(_parameter, _payslipInst, _date, _dueDate, _emplInst);
            }
        }
        if (ret == null) {
            ret = getDefaultValue(_parameter, _templInst, CIPayroll.TemplatePayslip.DefaultHolidayLaborTime);
        }
        return ret;
    }

    protected BigDecimal getDefaultValue(final Parameter _parameter,
                                         final Instance _templInst,
                                         final CIAttribute _attr)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (_templInst != null && _templInst.isValid()) {
            final PrintQuery print = CachedPrintQuery.get4Request(_templInst);
            print.addAttribute(CIPayroll.TemplatePayslip.DefaultLaborTime,
                            CIPayroll.TemplatePayslip.DefaultExtraLaborTime,
                            CIPayroll.TemplatePayslip.DefaultHolidayLaborTime,
                            CIPayroll.TemplatePayslip.DefaultNightLaborTime);
            print.execute();
            final Object[] obj = print.getAttribute(_attr);
            if (obj != null) {
                final BigDecimal val = (BigDecimal) obj[0];
                final UoM uoM = (UoM) obj[1];
                if (uoM.equals(uoM.getDimension().getBaseUoM())) {
                    ret = val;
                } else {
                    ret = val.multiply(new BigDecimal(uoM.getNumerator())).setScale(8, BigDecimal.ROUND_HALF_UP)
                                    .divide(new BigDecimal(uoM.getDenominator()), BigDecimal.ROUND_HALF_UP);
                }
            }
        }
        return ret != null && ret.compareTo(BigDecimal.ZERO) > -1 ? ret : null;
    }

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

    protected void connect2Project(final Parameter _parameter,
                                   final CreatedDoc _createdDoc,
                                   final Instance _emplInst)
        throws EFapsException
    {
        try {
            if (AppDependency.getAppDependency("eFapsApp-Projects").isMet()) {
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


    protected void updateTotals(final Parameter _parameter,
                                final Instance _docInst,
                                final Result _result,
                                final Instance _rateCurInst,
                                final Object[] _rateObj)
        throws EFapsException
    {
        final Instance baseCurIns = Currency.getBaseCurrency();
        final BigDecimal rate = ((BigDecimal) _rateObj[0]).divide((BigDecimal) _rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);

        final BigDecimal rateCrossTotal = _result.getTotal()
                        .setScale(_result.getFormatter().getMaximumFractionDigits(), BigDecimal.ROUND_HALF_UP);

        final BigDecimal crossTotal = rateCrossTotal.divide(rate, BigDecimal.ROUND_HALF_UP)
                        .setScale(_result.getFormatter().getMaximumFractionDigits(), BigDecimal.ROUND_HALF_UP);

        final BigDecimal cost = _result.getCost().divide(rate, BigDecimal.ROUND_HALF_UP)
                        .setScale(_result.getFormatter().getMaximumFractionDigits(), BigDecimal.ROUND_HALF_UP);

        final Update insert = new Update(_docInst);
        insert.add(CIPayroll.Payslip.RateCrossTotal, rateCrossTotal);
        insert.add(CIPayroll.Payslip.Rate, _rateObj);
        insert.add(CIPayroll.Payslip.CrossTotal, crossTotal);
        insert.add(CIPayroll.Payslip.AmountCost, cost);
        insert.add(CIPayroll.Payslip.CurrencyId, baseCurIns);
        insert.add(CIPayroll.Payslip.RateCurrencyId, _rateCurInst);
        insert.execute();

        for (final AbstractRule<?> rule : _result.getRules()) {
            if (rule.add()) {
                for (final IDocRuleListener listener : rule.getRuleListeners(IDocRuleListener.class)) {
                    listener.execute(_parameter, _docInst);
                }
            }
        }

    }

    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final Instance instance = _parameter.getInstance();

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
        update.add(CIPayroll.Payslip.LaborTime, new Object[] { laborTime, laborTimeUoM });
        update.add(CIPayroll.Payslip.ExtraLaborTime, new Object[] { extraLaborTime, extraLaborTimeUoM });
        update.add(CIPayroll.Payslip.NightLaborTime, new Object[] { nightLaborTime, nightLaborTimeUoM });
        update.add(CIPayroll.Payslip.HolidayLaborTime, new Object[] { holidayLaborTime, holidayLaborTimeUoM });
        update.add(CIPayroll.Payslip.Date, date);
        update.add(CIPayroll.Payslip.DueDate, dueDate);
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

    protected void updatePositions(final Parameter _parameter,
                                   final Instance _docInst,
                                   final Result _result,
                                   final Instance _rateCurInst,
                                   final Object[] _rateObj)
        throws EFapsException
    {
        final BigDecimal rate = ((BigDecimal) _rateObj[0]).divide((BigDecimal) _rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);
        final Instance baseCurIns = Currency.getBaseCurrency();

        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink, _docInst);
        final InstanceQuery query = queryBldr.getQuery();
        final List<Instance> posInsts = query.executeWithoutAccessCheck();
        final Iterator<Instance> posIter = posInsts.iterator();
        int idx = 1;
        for (final AbstractRule<?> rule : _result.getRules()) {
            if (rule.add()) {
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

                final BigDecimal rateAmount = _result.getBigDecimal(rule.getResult())
                                .setScale(_result.getFormatter().getMaximumFractionDigits(), BigDecimal.ROUND_HALF_UP);

                final BigDecimal amount = rateAmount.divide(rate, BigDecimal.ROUND_HALF_UP)
                                .setScale(_result.getFormatter().getMaximumFractionDigits(), BigDecimal.ROUND_HALF_UP);

                update.add(CIPayroll.PositionAbstract.RuleAbstractLink, rule.getInstance());
                update.add(CIPayroll.PositionAbstract.PositionNumber, idx);
                update.add(CIPayroll.PositionAbstract.Description, rule.getDescription());
                update.add(CIPayroll.PositionAbstract.Key, rule.getKey());
                update.add(CIPayroll.PositionAbstract.RateAmount, rateAmount);
                update.add(CIPayroll.PositionAbstract.Rate, _rateObj);
                update.add(CIPayroll.PositionAbstract.Amount, amount);
                update.add(CIPayroll.PositionAbstract.CurrencyLink, baseCurIns);
                update.add(CIPayroll.PositionAbstract.RateCurrencyLink, _rateCurInst);
                update.execute();
                idx++;
            }
        }

        while (posIter.hasNext()) {
            new Delete(posIter.next()).execute();
        }
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
                    map.put("extraLaborTime", formatter.format(laborTime));
                }
                final BigDecimal nightLaborTime = getDefaultValue(_parameter, templInst,
                                CIPayroll.TemplatePayslip.DefaultNightLaborTime);
                if (nightLaborTime != null) {
                    map.put("nightLaborTime", formatter.format(laborTime));
                }
                final BigDecimal holidayLaborTime = getDefaultValue(_parameter, templInst,
                                CIPayroll.TemplatePayslip.DefaultHolidayLaborTime);
                if (holidayLaborTime != null) {
                    map.put("holidayLaborTime", formatter.format(laborTime));
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
            final Instance instance = Instance.get(_parameter.getParameterValue(field.getName()));
            if (instance.isValid()) {
                map.put("employeeData", getFieldValue4Employee(instance));
                try {
                    if (AppDependency.getAppDependency("eFapsApp-Projects").isMet()) {
                        final QueryBuilder queryBldr = new QueryBuilder(CIProjects.ProjectService2Employee);
                        queryBldr.addWhereAttrEqValue(CIProjects.ProjectService2Employee.ToLink, instance);
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
                    listener.add2UpdateMap4Employee(_parameter, instance, map);
                }
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
        map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        list.add(map);

        for (final AbstractRule<?> rule : rules) {
            final Map<String, Object> map2 = new HashMap<>();
            list.add(map2);
            map2.put(EFapsKey.FIELDUPDATE_USEIDX.getKey(), ruleInsts.get(rule.getInstance()));
            map2.put(CITablePayroll.Payroll_PositionRuleTable.ruleAmount.name, rule.getResult());
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
                final String amountStr = amounts[_ruleInsts.get(rule.getInstance())];
                try {
                    final BigDecimal amount;
                    if (amountStr.isEmpty()) {
                        amount = BigDecimal.ZERO;
                    } else {
                        amount = (BigDecimal) NumberFormatter.get().getTwoDigitsFormatter()
                                        .parse(amountStr);
                    }
                    rule.setExpression(Calculator.toJexlBigDecimal(_parameter, amount));;
                } catch (final ParseException e) {
                    LOG.error("Catched ParserException", e);
                }
            }
        }
        Calculator.evaluate(_parameter, ret);
        return ret;
    }

    protected CharSequence getHtml4Positions(final Parameter _parameter,
                                             final List<? extends AbstractRule<?>> rules)
        throws EFapsException
    {
        final Result result = Calculator.getResult(_parameter, rules);
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

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing Snipplet
     * @throws EFapsException on error
     */
    @SuppressWarnings("unchecked")
    public Return getSelection4EditMassiveUIFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<DropDownPosition> values = new ArrayList<DropDownPosition>();
        List<Instance> insts = getSelectedInstances(_parameter);
        if (!insts.isEmpty()) {
            final MultiPrintQuery multi = new MultiPrintQuery(insts);
            multi.addAttribute(CIPayroll.DocumentAbstract.StatusAbstract);
            multi.execute();
            insts = new ArrayList<>();
            while (multi.next()) {
                final Status status = Status.get(multi.<Long>getAttribute(CIPayroll.DocumentAbstract.StatusAbstract));
                if (status.equals(Status.find(CIPayroll.PayslipStatus.Draft))
                                || status.equals(Status.find(CIPayroll.AdvanceStatus.Draft))
                                || status.equals(Status.find(CIPayroll.SettlementStatus.Draft))) {
                    insts.add(multi.getCurrentInstance());
                }
            }
            Context.getThreadContext().setSessionAttribute(Payslip.SESSIONKEY, insts);
        } else {
            insts = (List<Instance>) Context.getThreadContext().getSessionAttribute(Payslip.SESSIONKEY);
        }

        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.RuleInput);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.RuleInput.Key, CIPayroll.RuleInput.Description);
        multi.execute();
        while (multi.next()) {
            final String option = multi.getAttribute(CIPayroll.RuleInput.Key) + " - "
                            + multi.getAttribute(CIPayroll.RuleInput.Description);
            final DropDownPosition position = new org.efaps.esjp.common.uiform.Field().getDropDownPosition(
                            _parameter, multi.getCurrentInstance().getOid(), option);
            values.add(position);
        }

        values.add(0, new org.efaps.esjp.common.uiform.Field().getDropDownPosition(_parameter,
                        "HolidayLaborTime", DBProperties.getProperty("Payroll_Payslip/HolidayLaborTime.Label")));
        values.add(0, new org.efaps.esjp.common.uiform.Field().getDropDownPosition(_parameter,
                        "NightLaborTime", DBProperties.getProperty("Payroll_Payslip/NightLaborTime.Label")));
        values.add(0, new org.efaps.esjp.common.uiform.Field().getDropDownPosition(_parameter,
                        "ExtraLaborTime", DBProperties.getProperty("Payroll_Payslip/ExtraLaborTime.Label")));
        final DropDownPosition labeOp = new org.efaps.esjp.common.uiform.Field().getDropDownPosition(_parameter,
                        "LaborTime", DBProperties.getProperty("Payroll_Payslip/LaborTime.Label"));
        labeOp.setSelected(true);
        values.add(0, labeOp);

        ret.put(ReturnValues.VALUES, values);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing Snipplet
     * @throws EFapsException on error
     */
    public Return getValues4EditMassiveUIFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Table table = new Table();
        @SuppressWarnings("unchecked")
        final List<Instance> insts = (List<Instance>) Context.getThreadContext()
                        .getSessionAttribute(Payslip.SESSIONKEY);

        if (insts != null && !insts.isEmpty()) {
            final String selected = _parameter
                            .getParameterValue(CIFormPayroll.Payroll_PayslipEditMassiveSelectForm.select.name);
            final Map<String, String> map = new HashMap<>();
            switch (selected) {
                case "HolidayLaborTime":
                case "ExtraLaborTime":
                case "NightLaborTime":
                case "LaborTime":
                    table.addRow().addColumn(DBProperties.getProperty("Payroll_Payslip/" + selected + ".Label")
                                    + "<span id=\"btn\"><span>")
                        .getCurrentColumn().setStyle("font-weight: bold;");
                    final MultiPrintQuery multi = new MultiPrintQuery(insts);
                    final SelectBuilder selEmp = SelectBuilder.get()
                                    .linkto(CIPayroll.Payslip.EmployeeAbstractLink);
                    final SelectBuilder selEmpFirstName = new SelectBuilder(selEmp)
                                    .attribute(CIHumanResource.Employee.FirstName);
                    final SelectBuilder selEmpLastName = new SelectBuilder(selEmp)
                                    .attribute(CIHumanResource.Employee.LastName);
                    final SelectBuilder selEmpSecLastName = new SelectBuilder(selEmp)
                                    .attribute(CIHumanResource.Employee.SecondLastName);
                    final SelectBuilder selUoM = SelectBuilder.get().attribute(selected).label();
                    final SelectBuilder selTime = SelectBuilder.get().attribute(selected).value();
                    multi.addSelect(selEmpFirstName, selEmpLastName, selEmpSecLastName, selTime, selUoM);
                    multi.addAttribute(CIPayroll.Payslip.Name);
                    multi.setEnforceSorted(true);
                    multi.execute();
                    while (multi.next()) {
                        final String employee = multi.getSelect(selEmpLastName) + " "
                                        + multi.getSelect(selEmpSecLastName) + ", " + multi.getSelect(selEmpFirstName);
                        final String id1 = RandomStringUtils.randomAlphanumeric(8);
                        final String id2 = RandomStringUtils.randomAlphanumeric(8);
                        map.put(id1, id2);
                        table.addRow().addColumn("<input type=\"checkbox\" name=\"oid\" value=\""
                                            + multi.getCurrentInstance().getOid() + "\" id=\"" + id1 + "\" >")
                            .addColumn(multi.<String>getAttribute(CIPayroll.Payslip.Name))
                            .addColumn(employee)
                            .addColumn("<input disabled=\"disabled\" name=\"newValue\" value=\""
                                            + multi.getSelect(selTime) + "\" id=\"" + id2 + "\"></input>")
                            .addColumn(multi.<String>getSelect(selUoM));
                    }
                    break;
                default:
                    final PrintQuery print = new PrintQuery(selected);
                    print.addAttribute(CIPayroll.RuleAbstract.Key, CIPayroll.RuleAbstract.Description);
                    print.execute();
                    final String key = print.getAttribute(CIPayroll.RuleAbstract.Key);
                    table.addRow().addColumn(key + " - " + print.getAttribute(CIPayroll.RuleAbstract.Description)
                                    + "<span id=\"btn\"><span>")
                        .getCurrentColumn().setStyle("font-weight: bold;");

                    final MultiPrintQuery paySlipMulti = new MultiPrintQuery(insts);
                    final SelectBuilder selEmp2 = SelectBuilder.get()
                                    .linkto(CIPayroll.Payslip.EmployeeAbstractLink);
                    final SelectBuilder selEmpFirstName2 = new SelectBuilder(selEmp2)
                                    .attribute(CIHumanResource.Employee.FirstName);
                    final SelectBuilder selEmpLastName2 = new SelectBuilder(selEmp2)
                                    .attribute(CIHumanResource.Employee.LastName);
                    final SelectBuilder selEmpSecLastName2 = new SelectBuilder(selEmp2)
                                    .attribute(CIHumanResource.Employee.SecondLastName);
                    final SelectBuilder selCurrSymb =  SelectBuilder.get()
                                    .linkto(CIPayroll.Payslip.RateCurrencyId)
                                    .attribute(CIERP.Currency.Symbol);
                    paySlipMulti.addSelect(selCurrSymb, selEmpFirstName2, selEmpLastName2, selEmpSecLastName2);
                    paySlipMulti.addAttribute(CIPayroll.DocumentAbstract.Name);
                    paySlipMulti.setEnforceSorted(true);
                    paySlipMulti.execute();

                    while (paySlipMulti.next()) {
                        final String employee = paySlipMulti.getSelect(selEmpLastName2) + " "
                                        + paySlipMulti.getSelect(selEmpSecLastName2) + ", "
                                        + paySlipMulti.getSelect(selEmpFirstName2);

                        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
                        queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.Key, key);
                        queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink,
                                        paySlipMulti.getCurrentInstance());

                        final MultiPrintQuery multi2 = queryBldr.getPrint();
                        multi2.addAttribute(CIPayroll.PositionAbstract.RateAmount);
                        multi2.execute();
                        BigDecimal rateAmount;
                        String oid;
                        if (multi2.next()) {
                            rateAmount = multi2.<BigDecimal>getAttribute(CIPayroll.PositionAbstract.RateAmount);
                            oid = multi2.getCurrentInstance().getOid();

                        } else {
                            rateAmount = BigDecimal.ZERO;
                            oid = paySlipMulti.getCurrentInstance().getOid();
                        }

                        final String id1 = RandomStringUtils.randomAlphanumeric(8);
                        final String id2 = RandomStringUtils.randomAlphanumeric(8);
                        map.put(id1, id2);

                        table.addRow().addColumn("<input type=\"checkbox\" name=\"oid\" value=\""
                                        + oid + "\" id=\"" + id1 + "\" >")
                            .addColumn(paySlipMulti.<String>getAttribute(CIPayroll.DocumentAbstract.Name))
                            .addColumn(employee)
                            .addColumn("<input disabled=\"disabled\" name=\"newValue\" value=\""
                                            + rateAmount + "\" id=\"" + id2 + "\"></input>")
                            .addColumn(paySlipMulti.<String>getSelect(selCurrSymb));
                    }
                    break;
            }
            final StringBuilder js = new StringBuilder();

            for (final Entry<String, String> entry  :map.entrySet()) {
                js.append("on(dom.byId(\"").append(entry.getKey()).append("\"), \"click\", function(evt){")
                    .append("dom.byId(\"").append(entry.getValue())
                    .append("\").disabled = evt.currentTarget.checked ? '' : 'disabled';")
                    .append("});\n");
            }
            js.append( "  new ToggleButton({\n" +
                            "    showLabel: true,\n" +
                            "    label: 'activar todos',\n" +
                            "    checked: false,\n" +
                            "    onChange: function (val) {\n" +
                            "      this.set('label', val ? 'desactivar todos' : 'activar todos' );\n" +
                            "      query('input[type=checkbox]').forEach(function (node) {\n" +
                            "        node.checked = val;\n" +
                            "      });\n" +
                            "      query('[name=\\'newValue\\']').forEach(function (node) {\n" +
                            "        node.disabled = !val;\n" +
                            "      })\n" +
                            "    }\n" +
                            "  }, 'btn').startup();\n" +
                            "\n" +
                            "");

            final StringBuilder html = InterfaceUtils.wrappInScriptTag(_parameter,
                            InterfaceUtils.wrapInDojoRequire(_parameter, js, DojoLibs.ON, DojoLibs.DOM, DojoLibs.QUERY,
                                            DojoLibs.TOGGLEBUTTON), true, 1500);
            html.insert(0, table.toHtml()).append("<input type=\"hidden\" name=\"selected\" value=\"")
                .append(selected).append("\">");
            ret.put(ReturnValues.SNIPLETT, html);
        }

        return ret;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @return edit
     * @throws EFapsException on error
     */
    public Return edit4Massive(final Parameter _parameter)
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
                print.addAttribute(CIPayroll.Payslip.Rate, CIPayroll.Payslip.Date,
                                CIPayroll.Payslip.LaborTime, CIPayroll.Payslip.HolidayLaborTime,
                                CIPayroll.Payslip.ExtraLaborTime, CIPayroll.Payslip.NightLaborTime);
                print.execute();

                final Instance rateCurrInst = print.getSelect(selRateCurInst);
                final Object[] rateObj = print.getAttribute(CIPayroll.Payslip.Rate);
                final Object[] laborTime = print.getAttribute(CIPayroll.Payslip.LaborTime);
                final Object[] extraLaborTime = print.getAttribute(CIPayroll.Payslip.ExtraLaborTime);
                final Object[] holidayLaborTime = print.getAttribute(CIPayroll.Payslip.HolidayLaborTime);
                final Object[] nightLaborTime = print.getAttribute(CIPayroll.Payslip.NightLaborTime);

                final Update update = new Update(docInst);
                if (selected.equals("LaborTime")) {
                    update.add(CIPayroll.Payslip.LaborTime, new Object[] { values[i], laborTime[1] });
                }
                if (selected.equals("ExtraLaborTime")) {
                    update.add(CIPayroll.Payslip.ExtraLaborTime, new Object[] { values[i], extraLaborTime[1] });
                }
                if (selected.equals("HolidayLaborTime")) {
                    update.add(CIPayroll.Payslip.HolidayLaborTime, new Object[] { values[i], holidayLaborTime[1] });
                }
                if (selected.equals("NightLaborTime")) {
                    update.add(CIPayroll.Payslip.NightLaborTime, new Object[] { values[i], nightLaborTime[1] });
                }
                update.execute();

                final List<? extends AbstractRule<?>> rules = analyseRulesFomDoc(_parameter, docInst, mapping);
                final Result result = Calculator.getResult(_parameter, rules);
                updateTotals(_parameter, docInst, result, rateCurrInst, rateObj);
                updatePositions(_parameter, docInst, result, rateCurrInst, rateObj);

                final EditedDoc editedDoc = new EditedDoc(docInst);
                createReport(_parameter, editedDoc);
            }
        }
        return new Return();
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _docInst      Instance of the document
     * @return list of rules
     * @throws EFapsException on error
     */
    protected List<? extends AbstractRule<?>> analyseRulesFomDoc(final Parameter _parameter,
                                                                 final Instance _docInst)
        throws EFapsException
    {
        return analyseRulesFomDoc(_parameter, _docInst, new HashMap<Instance, BigDecimal>());
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _docInst       Instance of the document
     * @param _mapping      Map values
     * @return list of rules
     * @throws EFapsException on error
     */
    protected List<? extends AbstractRule<?>> analyseRulesFomDoc(final Parameter _parameter,
                                                                 final Instance _docInst,
                                                                 final Map<Instance, BigDecimal> _mapping)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_docInst);
        final SelectBuilder selTmplInst = SelectBuilder.get().linkto(CIPayroll.DocumentAbstract.TemplateLinkAbstract)
                        .instance();
        print.addSelect(selTmplInst);
        print.execute();

        final List<? extends AbstractRule<?>> ret = Template.getRules4Template(_parameter,
                        print.<Instance>getSelect(selTmplInst));

        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink, _docInst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selRulsInst = SelectBuilder.get().linkto(CIPayroll.PositionAbstract.RuleAbstractLink)
                        .instance();
        multi.addSelect(selRulsInst);
        multi.addAttribute(CIPayroll.PositionAbstract.RateAmount);
        multi.execute();
        final Map<Instance, BigDecimal> map = new HashMap<>();
        while (multi.next()) {
            final BigDecimal amount;
            if (_mapping.containsKey(multi.getCurrentInstance())) {
                amount = _mapping.get(multi.getCurrentInstance());
            } else {
                amount = multi.<BigDecimal>getAttribute(CIPayroll.PositionAbstract.RateAmount);
            }
            map.put(multi.<Instance>getSelect(selRulsInst), amount);
        }

        for (final AbstractRule<?> rule : ret) {
            if (rule instanceof InputRule) {
                BigDecimal amount = BigDecimal.ZERO;
                if (map.containsKey(rule.getInstance())) {
                    amount = map.get(rule.getInstance());
                }
                if (_mapping.containsKey(rule.getInstance())) {
                    amount = _mapping.get(rule.getInstance());
                }
                rule.setExpression(Calculator.toJexlBigDecimal(_parameter, amount));
            }
        }
        Calculator.evaluate(_parameter, ret, _docInst);
        return ret;
    }

}
