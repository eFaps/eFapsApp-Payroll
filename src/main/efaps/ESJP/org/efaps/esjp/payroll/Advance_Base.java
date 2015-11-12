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
 */

package org.efaps.esjp.payroll;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.ci.CIAttribute;
import org.efaps.ci.CIType;
import org.efaps.db.AttributeQuery;
import org.efaps.db.CachedPrintQuery;
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
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.payroll.basis.BasisAttribute;
import org.efaps.esjp.payroll.listener.IOnAdvance;
import org.efaps.esjp.payroll.rules.AbstractRule;
import org.efaps.esjp.payroll.rules.Calculator;
import org.efaps.esjp.payroll.rules.Result;
import org.efaps.esjp.payroll.util.Payroll;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.document.AbstractDocument;
import org.efaps.update.AppDependency;
import org.efaps.update.util.InstallationException;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("973456d7-c147-4f3b-893e-0f7852d4d084")
@EFapsApplication("eFapsApp-Payroll")
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
        final DateTime date = new DateTime(_parameter.getParameterValue(CIFormPayroll.Payroll_AdvanceForm.date.name));
        final String docType = _parameter.getParameterValue(CIFormPayroll.Payroll_AdvanceForm.docType.name);
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
                    final Instance emplInst = Instance.get(employees[i]);
                    final String name = getDocName4Create(_parameter);

                    final BigDecimal lT = getLaborTime(_parameter, null, date, emplInst, null);
                    final BigDecimal elT = getExtraLaborTime(_parameter, null, date, emplInst, null);
                    final BigDecimal nlT = getNightLaborTime(_parameter, null, date, emplInst, null);
                    final BigDecimal hlT = getHolidayLaborTime(_parameter, null, date, emplInst, null);

                    insert.add(CIPayroll.Advance.Name, name);
                    insert.add(CIPayroll.Advance.DocType, docType);
                    insert.add(CIPayroll.Advance.Date, date);
                    insert.add(CIPayroll.Advance.EmployeeAbstractLink, emplInst);
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
                    insert.add(CIPayroll.Advance.LaborTime,
                                    new Object[] { lT == null ? BigDecimal.ZERO : lT, timeDim.getBaseUoM().getId() });
                    insert.add(CIPayroll.Advance.ExtraLaborTime,
                                    new Object[] { elT == null ? BigDecimal.ZERO : elT, timeDim.getBaseUoM().getId() });
                    insert.add(CIPayroll.Advance.NightLaborTime,
                                    new Object[] { nlT == null ? BigDecimal.ZERO : nlT, timeDim.getBaseUoM().getId() });
                    insert.add(CIPayroll.Advance.HolidayLaborTime,
                                    new Object[] { hlT == null ? BigDecimal.ZERO : hlT, timeDim.getBaseUoM().getId() });
                    insert.add(CIPayroll.Advance.Basis, BasisAttribute.getValueList4Inst(_parameter, emplInst));
                    insert.execute();
                }
            }
        } catch (final ParseException e) {
            throw new EFapsException(Payslip_Base.class, "createMassive.ParseException", e);
        }
        return new Return();
    }

    public Return evaluateTimes(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> advanceInsts = new ArrayList<>();

        final Instance tmpinst = _parameter.getInstance();
        if (tmpinst != null && tmpinst.isValid()) {
            advanceInsts.add(tmpinst);
        } else {
            advanceInsts.addAll(getSelectedInstances(_parameter));
        }

        for (final Instance advanceInst : advanceInsts) {

            final PrintQuery print = new PrintQuery(advanceInst);
            final SelectBuilder selEmplInst = SelectBuilder.get().linkto(CIPayroll.Advance.EmployeeAbstractLink)
                            .instance();
            final SelectBuilder selTemplInst = SelectBuilder.get().linkto(CIPayroll.Payslip.TemplateLink)
                            .instance();
            print.addSelect(selEmplInst, selTemplInst);
            print.addAttribute(CIPayroll.Advance.Date, CIPayroll.Advance.Status);
            print.execute();
            final Status status = Status.get(print.<Long>getAttribute(CIPayroll.Advance.Status));
            if (Status.find(CIPayroll.AdvanceStatus.Draft).equals(status)) {
                final DateTime date = print.getAttribute(CIPayroll.Advance.Date);
                final Instance emplInst = print.getSelect(selEmplInst);
                final Instance templInst = print.getSelect(selTemplInst);

                final Dimension timeDim = CIPayroll.Advance.getType().getAttribute(CIPayroll.Advance.LaborTime.name)
                                .getDimension();

                final BigDecimal lT = getLaborTime(_parameter, advanceInst, date, emplInst, templInst);
                final BigDecimal elT = getExtraLaborTime(_parameter, advanceInst, date, emplInst, templInst);
                final BigDecimal nlT = getNightLaborTime(_parameter, advanceInst, date, emplInst, templInst);
                final BigDecimal hlT = getHolidayLaborTime(_parameter, advanceInst, date, emplInst, templInst);

                if (lT != null || elT != null || nlT != null || hlT != null) {
                    final Update update = new Update(advanceInst);
                    if (lT != null) {
                        update.add(CIPayroll.Advance.LaborTime, new Object[] { lT, timeDim.getBaseUoM().getId() });
                    }
                    if (elT != null) {
                        update.add(CIPayroll.Advance.ExtraLaborTime, new Object[] { elT, timeDim.getBaseUoM().getId() });
                    }
                    if (nlT != null) {
                        update.add(CIPayroll.Advance.NightLaborTime, new Object[] { nlT, timeDim.getBaseUoM().getId() });
                    }
                    if (hlT != null) {
                        update.add(CIPayroll.Advance.HolidayLaborTime,
                                        new Object[] { hlT, timeDim.getBaseUoM().getId() });
                    }
                    update.execute();
                }
            }
        }
        return new Return();
    }

    protected BigDecimal getLaborTime(final Parameter _parameter,
                                      final Instance _advanceInst,
                                      final DateTime _date,
                                      final Instance _emplInst,
                                      final Instance _templInst)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (Payroll.ADVANCEEVALLABORTIME.get()) {
            for (final IOnAdvance listener : Listener.get().<IOnAdvance>invoke(IOnAdvance.class)) {
                ret = listener.getLaborTime(_parameter, _advanceInst, _date, _emplInst);
            }
        }
        if (ret == null) {
            ret = getDefaultValue(_parameter, _templInst, CIPayroll.TemplateAdvance.DefaultLaborTime);
        }
        return ret;
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
     * Gets the default value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _templInst the templ inst
     * @param _attr the attr
     * @return the default value
     * @throws EFapsException on error
     */
    protected BigDecimal getDefaultValue(final Parameter _parameter,
                                         final Instance _templInst,
                                         final CIAttribute _attr)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (_templInst != null && _templInst.isValid()) {
            final PrintQuery print = CachedPrintQuery.get4Request(_templInst);
            print.addAttribute(CIPayroll.TemplateAdvance.DefaultLaborTime,
                            CIPayroll.TemplateAdvance.DefaultExtraLaborTime,
                            CIPayroll.TemplateAdvance.DefaultHolidayLaborTime,
                            CIPayroll.TemplateAdvance.DefaultNightLaborTime);
            print.execute();
            ret = print.getAttribute(_attr);
        }
        return ret != null && ret.compareTo(BigDecimal.ZERO) > -1 ? ret : null;
    }

    protected BigDecimal getExtraLaborTime(final Parameter _parameter,
                                           final Instance _advanceInst,
                                           final DateTime _date,
                                           final Instance _emplInst,
                                           final Instance _templInst)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (Payroll.ADVANCEEVALEXTRALABORTIME.get()) {
            for (final IOnAdvance listener : Listener.get().<IOnAdvance>invoke(IOnAdvance.class)) {
                ret = listener.getExtraLaborTime(_parameter, _advanceInst, _date, _emplInst);
            }
        }
        if (ret == null) {
            ret = getDefaultValue(_parameter, _templInst, CIPayroll.TemplateAdvance.DefaultExtraLaborTime);
        }
        return ret;
    }

    protected BigDecimal getNightLaborTime(final Parameter _parameter,
                                           final Instance _advanceInst,
                                           final DateTime _date,
                                           final Instance _emplInst,
                                           final Instance _templInst)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (Payroll.ADVANCEEVALNIGHTLABORTIME.get()) {
            for (final IOnAdvance listener : Listener.get().<IOnAdvance>invoke(IOnAdvance.class)) {
                ret = listener.getNightLaborTime(_parameter, _advanceInst, _date, _emplInst);
            }
        }
        if (ret == null) {
            ret = getDefaultValue(_parameter, _templInst, CIPayroll.TemplateAdvance.DefaultNightLaborTime);
        }
        return ret;
    }

    protected BigDecimal getHolidayLaborTime(final Parameter _parameter,
                                             final Instance _advanceInst,
                                             final DateTime _date,
                                             final Instance _emplInst,
                                             final Instance _templInst)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (Payroll.ADVANCEEVALHOLIDAYLABORTIME.get()) {
            for (final IOnAdvance listener : Listener.get().<IOnAdvance>invoke(IOnAdvance.class)) {
                ret = listener.getHolidayLaborTime(_parameter, _advanceInst, _date, _emplInst);
            }
        }
        if (ret == null) {
            ret = getDefaultValue(_parameter, _templInst, CIPayroll.TemplateAdvance.DefaultHolidayLaborTime);
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
    public Return editAmount(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();

        final String date = _parameter.getParameterValue(CIFormPayroll.Payroll_AdvanceEditForm.date.name);
        final String crossTotalStr = _parameter
                        .getParameterValue(CIFormPayroll.Payroll_AdvanceEditForm.rateCrossTotal4Edit.name);

        final PrintQuery print = new PrintQuery(instance);
        print.addAttribute(CIPayroll.Advance.Rate);
        print.execute();
        final BigDecimal rate = new Currency().evalRate((Object[]) print.getAttribute(CIPayroll.Advance.Rate), false);

        BigDecimal rateCrossTotal = BigDecimal.ONE;
        try {
            rateCrossTotal = (BigDecimal) NumberFormatter.get().getFormatter().parse(crossTotalStr);
        } catch (final ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        final BigDecimal crossTotal = rateCrossTotal.divide(rate, BigDecimal.ROUND_HALF_UP).setScale(2,
                        BigDecimal.ROUND_HALF_UP);
        final Update update = new Update(instance);
        update.add(CIPayroll.Advance.Date, date);
        update.add(CIPayroll.Advance.RateCrossTotal, rateCrossTotal);
        update.add(CIPayroll.Advance.CrossTotal, crossTotal);
        update.add(CIPayroll.Advance.AmountCost, crossTotal);
        update.execute();

        return new Return();
    }

    /**
     * Create advances.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        new Return();

        final Instance instance = _parameter.getInstance();
        final String docType = _parameter.getParameterValue(CIFormPayroll.Payroll_AdvanceForm.docType.name);
        final String date = _parameter.getParameterValue(CIFormPayroll.Payroll_AdvanceForm.date.name);
        final String laborTime = _parameter.getParameterValue(CIFormPayroll.Payroll_AdvanceForm.laborTime.name);
        final String laborTimeUoM = _parameter.getParameterValue("laborTimeUoM");
        final String extraLaborTime = _parameter
                        .getParameterValue(CIFormPayroll.Payroll_AdvanceForm.extraLaborTime.name);
        final String extraLaborTimeUoM = _parameter.getParameterValue("extraLaborTimeUoM");
        final String nightLaborTime = _parameter
                        .getParameterValue(CIFormPayroll.Payroll_AdvanceForm.nightLaborTime.name);
        final String nightLaborTimeUoM = _parameter.getParameterValue("nightLaborTimeUoM");
        final String holidayLaborTime = _parameter
                        .getParameterValue(CIFormPayroll.Payroll_AdvanceForm.holidayLaborTime.name);
        final String holidayLaborTimeUoM = _parameter.getParameterValue("holidayLaborTimeUoM");

        final Update update = new Update(instance);
        update.add(CIPayroll.Advance.DocType, docType);
        update.add(CIPayroll.Advance.LaborTime, new Object[] { laborTime, laborTimeUoM });
        update.add(CIPayroll.Advance.ExtraLaborTime, new Object[] { extraLaborTime, extraLaborTimeUoM });
        update.add(CIPayroll.Advance.NightLaborTime, new Object[] { nightLaborTime, nightLaborTimeUoM });
        update.add(CIPayroll.Advance.HolidayLaborTime, new Object[] { holidayLaborTime, holidayLaborTimeUoM });
        update.add(CIPayroll.Advance.Date, date);
        update.add(CIPayroll.Advance.Basis, BasisAttribute.getValueList4Inst(_parameter, instance));
        update.execute();

        final PrintQuery print = new PrintQuery(instance);
        final SelectBuilder selRateCurInst = SelectBuilder.get().linkto(CIPayroll.Advance.RateCurrencyId).instance();
        print.addSelect(selRateCurInst);
        print.addAttribute(CIPayroll.Advance.Rate);
        print.execute();

        final Instance rateCurrInst = print.getSelect(selRateCurInst);
        final Object[] rateObj = print.getAttribute(CIPayroll.Advance.Rate);
        final Payslip payslip = new Payslip();

        final List<? extends AbstractRule<?>> rules = payslip.analyseRulesFomUI(_parameter,
                        payslip.getRuleInstFromUI(_parameter));
        final Result result = Calculator.getResult(_parameter, rules);

        payslip.updateTotals(_parameter, instance, result, rateCurrInst, rateObj);
        payslip.updatePositions(_parameter, instance, result, rateCurrInst, rateObj);

        return new Return();
    }

    public Return createMultiple(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> templates = getInstances(_parameter,
                        CIFormPayroll.Payroll_AdvanceCreateMultipleForm.templates.name);
        final DateTime date = new DateTime(_parameter.getParameterValue(
                        CIFormPayroll.Payroll_AdvanceCreateMultipleForm.date.name));

        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Template2EmployeeAbstract);
        queryBldr.addWhereAttrInQuery(CIPayroll.Template2EmployeeAbstract.ToAbstractLink,
                        getAttrQuery4Employees(_parameter, date.withDayOfMonth(1) , date));
        queryBldr.addWhereAttrEqValue(CIPayroll.Template2EmployeeAbstract.FromAbstractLink, templates.toArray());

        final MultiPrintQuery multi = queryBldr.getPrint();

        final SelectBuilder selTemplInst = SelectBuilder.get()
                        .linkto(CIPayroll.Template2EmployeeAbstract.FromAbstractLink).instance();
        final SelectBuilder selEmplInst = SelectBuilder.get()
                        .linkto(CIPayroll.Template2EmployeeAbstract.ToAbstractLink).instance();
        multi.addSelect(selTemplInst, selEmplInst);
        multi.execute();

        final String docType = _parameter.getParameterValue(
                        CIFormPayroll.Payroll_AdvanceCreateMultipleForm.docType.name);

        while (multi.next()) {
            final Instance templInst = multi.getSelect(selTemplInst);
            final Instance emplInst = multi.getSelect(selEmplInst);
            create(_parameter, templInst, emplInst, date, docType);
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
     * @param _docType the doc type
     * @throws EFapsException on error
     */
    protected void create(final Parameter _parameter,
                          final Instance _templInst,
                          final Instance _emplInst,
                          final DateTime _date,
                          final Object _docType)
        throws EFapsException
    {
        final Object[] rateObj = new Object[] { BigDecimal.ONE, BigDecimal.ONE };
        final Dimension timeDim = CIPayroll.Advance.getType().getAttribute(CIPayroll.Advance.LaborTime.name)
                        .getDimension();

        final CreatedDoc createdDoc = new CreatedDoc();

        final Insert insert = new Insert(CIPayroll.Advance);
        insert.add(CIPayroll.Advance.Name, getDocName4Create(_parameter));
        insert.add(CIPayroll.Advance.DocType, _docType);
        insert.add(CIPayroll.Advance.Date, getStartDate(_parameter, _date, _emplInst));
        insert.add(CIPayroll.Advance.EmployeeAbstractLink, _emplInst);
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
        insert.add(CIPayroll.Advance.TemplateLinkAbstract, _templInst);

        if (_parameter.getInstance() != null
                        && _parameter.getInstance().getType().isKindOf(CIPayroll.ProcessAbstract)) {
            insert.add(CIPayroll.Payslip.ProcessAbstractLink, _parameter.getInstance());
        }

        insert.execute();

        createdDoc.setInstance(insert.getInstance());
        connect2Project(_parameter, createdDoc, _emplInst);

        final List<? extends AbstractRule<?>> rules = Template.getRules4Template(_parameter, _templInst);
        Calculator.evaluate(_parameter, rules, createdDoc.getInstance());
        final Result result = Calculator.getResult(_parameter, rules);
        final Payslip payslip = new Payslip();
        payslip.updateTotals(_parameter, insert.getInstance(), result, Currency.getBaseCurrency(), rateObj);
        payslip.updatePositions(_parameter, insert.getInstance(), result, Currency.getBaseCurrency(), rateObj);
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
                update.add(CIPayroll.Advance.Basis, BasisAttribute.getValueList4Inst(_parameter, docInst));
                update.execute();

                final Payslip payslip = new Payslip();
                final List<? extends AbstractRule<?>> rules = payslip.analyseRulesFomDoc(_parameter, docInst, mapping);
                final Result result = Calculator.getResult(_parameter, rules);
                payslip.updateTotals(_parameter, docInst, result, rateCurrInst, rateObj);
                payslip.updatePositions(_parameter, docInst, result, rateCurrInst, rateObj);
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

        try {
            if (AppDependency.getAppDependency("eFapsApp-Projects").isMet()) {
                final QueryBuilder queryBldr2 = new QueryBuilder(CIPayroll.Projects_ProjectService2Advance);
                queryBldr2.addWhereAttrEqValue(CIPayroll.Projects_ProjectService2Advance.ToDocument,
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
                    final Insert insert = new Insert(CIPayroll.Projects_ProjectService2Advance);
                    insert.add(CIPayroll.Projects_ProjectService2Advance.FromLink, projInst);
                    insert.add(CIPayroll.Projects_ProjectService2Advance.ToLink, _createdDoc.getInstance());
                    insert.execute();
                }
            }
        } catch (final InstallationException e) {
            throw new EFapsException("Catched Error.", e);
        }
    }

    /**
     * Gets the attribute query 4 employees.
     * All active employees and inactive in the given timeframe.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _minDate the min date
     * @param _maxDate the max date
     * @return the att query4 employees
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
}
