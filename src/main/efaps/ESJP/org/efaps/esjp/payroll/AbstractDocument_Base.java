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

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
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
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.common.util.InterfaceUtils_Base.DojoLibs;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.WarningUtil;
import org.efaps.esjp.payroll.Payslip_Base.PayslipNoSelection4EditMassiveWarning;
import org.efaps.esjp.payroll.Payslip_Base.PayslipSelectionQuantity4EditMassiveWarning;
import org.efaps.esjp.payroll.basis.BasisAttribute;
import org.efaps.esjp.payroll.listener.IOnAdvance;
import org.efaps.esjp.payroll.listener.IOnPayslip;
import org.efaps.esjp.payroll.rules.AbstractRule;
import org.efaps.esjp.payroll.rules.Calculator;
import org.efaps.esjp.payroll.rules.IDocRuleListener;
import org.efaps.esjp.payroll.rules.InputRule;
import org.efaps.esjp.payroll.rules.Result;
import org.efaps.esjp.payroll.util.Payroll;
import org.efaps.esjp.sales.document.AbstractDocumentSum;
import org.efaps.esjp.ui.html.Table;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_Base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("230e3e3d-d7fd-4ae8-9995-0d5a412510cb")
@EFapsApplication("eFapsApp-Payroll")
public abstract class AbstractDocument_Base
    extends AbstractDocumentSum
{

    /**
     * Logger for this classes.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDocument.class);

    /**
     * Evaluate times.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return evaluateTimes(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> docInsts = new ArrayList<>();

        final Instance tmpinst = _parameter.getInstance();
        if (tmpinst != null && tmpinst.isValid()) {
            docInsts.add(tmpinst);
        } else {
            docInsts.addAll(getSelectedInstances(_parameter));
        }

        for (final Instance docInst : docInsts) {
            final PrintQuery print = new PrintQuery(docInst);
            final SelectBuilder selEmplInst = SelectBuilder.get().linkto(
                            CIPayroll.DocumentAbstract.EmployeeAbstractLink).instance();
            final SelectBuilder selTemplInst = SelectBuilder.get().linkto(
                            CIPayroll.DocumentAbstract.TemplateLinkAbstract).instance();
            print.addSelect(selEmplInst, selTemplInst);
            print.addAttribute(CIPayroll.DocumentAbstract.Date, CIPayroll.DocumentAbstract.DueDate,
                            CIPayroll.DocumentAbstract.StatusAbstract);
            print.execute();
            final Status status = Status.get(print.<Long>getAttribute(CIPayroll.DocumentAbstract.StatusAbstract));
            if (status.equals(getStatus4evaluateTimes(_parameter))) {
                final DateTime date = print.getAttribute(CIPayroll.DocumentAbstract.Date);
                final DateTime dueDate = print.getAttribute(CIPayroll.DocumentAbstract.DueDate);
                final Instance emplInst = print.getSelect(selEmplInst);
                final Instance templInst = print.getSelect(selTemplInst);

                final Dimension timeDim = CIPayroll.DocumentAbstract.getType()
                                .getAttribute(CIPayroll.DocumentAbstract.LaborTime.name).getDimension();

                final BigDecimal lT = getLaborTime(_parameter, docInst, date, dueDate, emplInst, templInst);
                final BigDecimal elT = getExtraLaborTime(_parameter, docInst, date, dueDate, emplInst, templInst);
                final BigDecimal nlT = getNightLaborTime(_parameter, docInst, date, dueDate, emplInst, templInst);
                final BigDecimal hlT = getHolidayLaborTime(_parameter, docInst, date, dueDate, emplInst, templInst);

                if (lT != null || elT != null || nlT != null || hlT != null) {
                    final Update update = new Update(docInst);
                    if (lT != null) {
                        update.add(CIPayroll.Payslip.LaborTime, new Object[] { lT, timeDim.getBaseUoM().getId() });
                    }
                    if (elT != null) {
                        update.add(CIPayroll.Payslip.ExtraLaborTime,
                                        new Object[] { elT, timeDim.getBaseUoM().getId() });
                    }
                    if (nlT != null) {
                        update.add(CIPayroll.Payslip.NightLaborTime,
                                        new Object[] { nlT, timeDim.getBaseUoM().getId() });
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

    /**
     * Gets the status4evaluate times.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the status4evaluate times
     * @throws EFapsException on error
     */
    protected Status getStatus4evaluateTimes(final Parameter _parameter)
        throws EFapsException
    {
        // to be used by child classes
        return null;
    }

    /**
     * Gets the labor time.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the payslip inst
     * @param _date the date
     * @param _dueDate the due date
     * @param _emplInst the empl inst
     * @param _templInst the templ inst
     * @return the labor time
     * @throws EFapsException on error
     */
    protected BigDecimal getLaborTime(final Parameter _parameter,
                                      final Instance _docInst,
                                      final DateTime _date,
                                      final DateTime _dueDate,
                                      final Instance _emplInst,
                                      final Instance _templInst)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (Payroll.PAYSLIPEVALLABORTIME.get() && _docInst.getType().isCIType(CIPayroll.Payslip)) {
            for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
                ret = listener.getLaborTime(_parameter, _docInst, _date, _dueDate, _emplInst);
            }
        }
        if (Payroll.ADVANCEEVALLABORTIME.get()  && _docInst.getType().isCIType(CIPayroll.Advance)) {
            for (final IOnAdvance listener : Listener.get().<IOnAdvance>invoke(IOnAdvance.class)) {
                ret = listener.getLaborTime(_parameter, _docInst, _date, _emplInst);
            }
        }
        if (ret == null) {
            ret = getDefaultValue(_parameter, _templInst, CIPayroll.TemplatePayslip.DefaultLaborTime);
        }
        return ret;
    }

    /**
     * Gets the extra labor time.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the payslip inst
     * @param _date the date
     * @param _dueDate the due date
     * @param _emplInst the empl inst
     * @param _templInst the templ inst
     * @return the extra labor time
     * @throws EFapsException on error
     */
    protected BigDecimal getExtraLaborTime(final Parameter _parameter,
                                           final Instance _docInst,
                                           final DateTime _date,
                                           final DateTime _dueDate,
                                           final Instance _emplInst,
                                           final Instance _templInst)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (Payroll.PAYSLIPEVALEXTRALABORTIME.get()  && _docInst.getType().isCIType(CIPayroll.Payslip)) {
            for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
                ret = listener.getExtraLaborTime(_parameter, _docInst, _date, _dueDate, _emplInst);
            }
        }
        if (Payroll.ADVANCEEVALEXTRALABORTIME.get()  && _docInst.getType().isCIType(CIPayroll.Advance)) {
            for (final IOnAdvance listener : Listener.get().<IOnAdvance>invoke(IOnAdvance.class)) {
                ret = listener.getExtraLaborTime(_parameter, _docInst, _date, _emplInst);
            }
        }
        if (ret == null) {
            ret = getDefaultValue(_parameter, _templInst, CIPayroll.TemplatePayslip.DefaultExtraLaborTime);
        }
        return ret;
    }

    /**
     * Gets the night labor time.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the payslip inst
     * @param _date the date
     * @param _dueDate the due date
     * @param _emplInst the empl inst
     * @param _templInst the templ inst
     * @return the night labor time
     * @throws EFapsException on error
     */
    protected BigDecimal getNightLaborTime(final Parameter _parameter,
                                           final Instance _docInst,
                                           final DateTime _date,
                                           final DateTime _dueDate,
                                           final Instance _emplInst,
                                           final Instance _templInst)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (Payroll.PAYSLIPEVALNIGHTLABORTIME.get()  && _docInst.getType().isCIType(CIPayroll.Payslip)) {
            for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
                ret = listener.getNightLaborTime(_parameter, _docInst, _date, _dueDate, _emplInst);
            }
        }
        if (Payroll.ADVANCEEVALNIGHTLABORTIME.get()  && _docInst.getType().isCIType(CIPayroll.Advance)) {
            for (final IOnAdvance listener : Listener.get().<IOnAdvance>invoke(IOnAdvance.class)) {
                ret = listener.getNightLaborTime(_parameter, _docInst, _date, _emplInst);
            }
        }
        if (ret == null) {
            ret = getDefaultValue(_parameter, _templInst, CIPayroll.TemplatePayslip.DefaultNightLaborTime);
        }
        return ret;
    }

    /**
     * Gets the holiday labor time.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the payslip inst
     * @param _date the date
     * @param _dueDate the due date
     * @param _emplInst the empl inst
     * @param _templInst the templ inst
     * @return the holiday labor time
     * @throws EFapsException on error
     */
    protected BigDecimal getHolidayLaborTime(final Parameter _parameter,
                                             final Instance _docInst,
                                             final DateTime _date,
                                             final DateTime _dueDate,
                                             final Instance _emplInst,
                                             final Instance _templInst)
        throws EFapsException
    {
        BigDecimal ret = null;
        if (Payroll.PAYSLIPEVALHOLIDAYLABORTIME.get() && _docInst.getType().isCIType(CIPayroll.Payslip)) {
            for (final IOnPayslip listener : Listener.get().<IOnPayslip>invoke(IOnPayslip.class)) {
                ret = listener.getHolidayLaborTime(_parameter, _docInst, _date, _dueDate, _emplInst);
            }
        }
        if (Payroll.ADVANCEEVALHOLIDAYLABORTIME.get()  && _docInst.getType().isCIType(CIPayroll.Advance)) {
            for (final IOnAdvance listener : Listener.get().<IOnAdvance>invoke(IOnAdvance.class)) {
                ret = listener.getHolidayLaborTime(_parameter, _docInst, _date, _emplInst);
            }
        }
        if (ret == null) {
            ret = getDefaultValue(_parameter, _templInst, CIPayroll.TemplatePayslip.DefaultHolidayLaborTime);
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
            print.addAttribute(CIPayroll.TemplateAbstract.DefaultLaborTime,
                            CIPayroll.TemplateAbstract.DefaultExtraLaborTime,
                            CIPayroll.TemplateAbstract.DefaultHolidayLaborTime,
                            CIPayroll.TemplateAbstract.DefaultNightLaborTime);
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
        queryBldr.addWhereAttrEqValue(CIPayroll.RuleInput.Status, Status.find(CIPayroll.RuleStatus.Active));
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
        Collections.sort(values, new Comparator<DropDownPosition>()
        {
            @Override
            public int compare(final DropDownPosition _o1,
                               final DropDownPosition _o2)
            {
                return _o1.getOrderValue().compareTo(_o2.getOrderValue());
            }
        });
        values.add(0, new org.efaps.esjp.common.uiform.Field().getDropDownPosition(_parameter,
                        "HolidayLaborTime", DBProperties.getProperty("Payroll_Payslip/HolidayLaborTime.Label")));
        values.add(0, new org.efaps.esjp.common.uiform.Field().getDropDownPosition(_parameter,
                        "NightLaborTime", DBProperties.getProperty("Payroll_Payslip/NightLaborTime.Label")));
        values.add(0, new org.efaps.esjp.common.uiform.Field().getDropDownPosition(_parameter,
                        "ExtraLaborTime", DBProperties.getProperty("Payroll_Payslip/ExtraLaborTime.Label")));
        values.add(0, new org.efaps.esjp.common.uiform.Field().getDropDownPosition(_parameter,
                        "LaborTime", DBProperties.getProperty("Payroll_Payslip/LaborTime.Label")));
        ret.put(ReturnValues.VALUES, values);
        return ret;
    }

    /**
     * Validate selection for edit massive.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return validateSelection4EditMassive(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<IWarning> warnings = new ArrayList<IWarning>();
        final String[] selected = _parameter
                        .getParameterValues(CIFormPayroll.Payroll_PayslipEditMassiveSelectForm.select.name);
        if (ArrayUtils.isEmpty(selected)) {
            warnings.add(new PayslipNoSelection4EditMassiveWarning());
        } else {
            @SuppressWarnings("unchecked")
            final List<Instance> insts = (List<Instance>) Context.getThreadContext().getSessionAttribute(
                            Payslip.SESSIONKEY);
            int maxqty;
            if (insts != null && !insts.isEmpty() && insts.get(0).getType().isCIType(CIPayroll.Advance)) {
                maxqty = Payroll.ADVANCEEDITMASSRULEQTY.get();
            } else {
                maxqty = Payroll.PAYSLIPEDITMASSRULEQTY.get();
            }
            if (selected.length > maxqty) {
                warnings.add(new PayslipSelectionQuantity4EditMassiveWarning().addObject(maxqty));
            }
        }
        if (warnings.isEmpty()) {
            ret.put(ReturnValues.TRUE, true);
        } else {
            ret.put(ReturnValues.SNIPLETT, WarningUtil.getHtml4Warning(warnings).toString());
            if (!WarningUtil.hasError(warnings)) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
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
            table.addRow().addColumn("<span id=\"btn\"><span>").getCurrentColumn().setColSpan(3);

            final String[] selectedArr = _parameter
                            .getParameterValues(CIFormPayroll.Payroll_PayslipEditMassiveSelectForm.select.name);
            final Map<String, Set<String>> map = new HashMap<>();

            final MultiPrintQuery multi = new MultiPrintQuery(insts);
            final SelectBuilder selCurrSymb =  SelectBuilder.get()
                            .linkto(CIPayroll.Payslip.RateCurrencyId)
                            .attribute(CIERP.Currency.Symbol);
            final SelectBuilder selEmployee = SelectBuilder.get().linkto(
                            CIPayroll.DocumentAbstract.EmployeeAbstractLink);
            //HumanResource_EmployeeMsgPhrase
            final MsgPhrase msgPhrase = MsgPhrase.get(UUID.fromString("f543ca6d-29fb-4f1a-8747-0057b9a08404"));
            multi.addMsgPhrase(selEmployee, msgPhrase);
            multi.addSelect(selCurrSymb);
            multi.addAttribute(CIPayroll.DocumentAbstract.Name);
            multi.setEnforceSorted(true);
            // add the first row and some selects
            for (final String selected : selectedArr) {
                switch (selected) {
                    case "HolidayLaborTime":
                    case "ExtraLaborTime":
                    case "NightLaborTime":
                    case "LaborTime":
                        final SelectBuilder selUoM = SelectBuilder.get().attribute(selected).label();
                        final SelectBuilder selTime = SelectBuilder.get().attribute(selected).value();
                        multi.addSelect(selTime, selUoM);
                        table.getCurrentRow()
                            .addColumn(DBProperties.getProperty("Payroll_Payslip/" + selected + ".Label"))
                            .getCurrentColumn().setStyle("font-weight: bold;").setColSpan(2);
                        break;
                    default:
                        final PrintQuery print = new PrintQuery(selected);
                        print.addAttribute(CIPayroll.RuleAbstract.Key, CIPayroll.RuleAbstract.Description);
                        print.execute();
                        final String key = print.getAttribute(CIPayroll.RuleAbstract.Key);
                        table.getCurrentRow()
                            .addColumn(key + " - " + print.getAttribute(CIPayroll.RuleAbstract.Description))
                            .getCurrentColumn().setStyle("font-weight: bold;").setColSpan(2);
                        break;
                }
            }
            multi.execute();
            while (multi.next()) {
                final String id1 = RandomStringUtils.randomAlphanumeric(8);
                map.put(id1, new HashSet<String>());
                final String employee = multi.getMsgPhrase(selEmployee, msgPhrase);
                table.addRow().addColumn("<input type=\"checkbox\" name=\"oid\" value=\""
                                + multi.getCurrentInstance().getOid() + "\" id=\"" + id1 + "\" >")
                    .addColumn(multi.<String>getAttribute(CIPayroll.Payslip.Name))
                    .addColumn(employee);

                for (final String selected : selectedArr) {
                    final String id2 = RandomStringUtils.randomAlphanumeric(8);
                    map.get(id1).add(id2);
                    switch (selected) {
                        case "HolidayLaborTime":
                        case "ExtraLaborTime":
                        case "NightLaborTime":
                        case "LaborTime":
                            final SelectBuilder selUoM = SelectBuilder.get().attribute(selected).label();
                            final SelectBuilder selTime = SelectBuilder.get().attribute(selected).value();
                            final String name = "newValue_" + selected;
                            table.getCurrentRow()
                                .addColumn("<input disabled=\"disabled\" name=\"" + name + "\" size=\"8\" value=\""
                                                + multi.getSelect(selTime) + "\" id=\"" + id2
                                                + "\" style=\"text-align: right;\"></input>")
                                .getCurrentColumn().setStyle("text-align: right;").getRow()
                                .addColumn(multi.<String>getSelect(selUoM));
                            break;
                        default:
                            final String name2 = "newValue_" + selected;
                            final PrintQuery print = new PrintQuery(selected);
                            print.addAttribute(CIPayroll.RuleAbstract.Key, CIPayroll.RuleAbstract.Description);
                            print.execute();
                            final String key = print.getAttribute(CIPayroll.RuleAbstract.Key);

                            final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
                            queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.Key, key);
                            queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink,
                                                multi.getCurrentInstance());
                            final MultiPrintQuery multi2 = queryBldr.getPrint();
                            multi2.addAttribute(CIPayroll.PositionAbstract.RateAmount);
                            multi2.execute();
                            BigDecimal rateAmount;
                            if (multi2.next()) {
                                rateAmount = multi2.<BigDecimal>getAttribute(CIPayroll.PositionAbstract.RateAmount);
                            } else {
                                rateAmount = BigDecimal.ZERO;
                            }
                            table.getCurrentRow()
                                .addColumn("<input disabled=\"disabled\" name=\"" + name2 + "\" size=\"8\" value=\""
                                                + rateAmount + "\" id=\"" + id2
                                                + "\" style=\"text-align: right;\"></input>")
                                .getCurrentColumn().setStyle("text-align: right;").getRow()
                                .addColumn(multi.<String>getSelect(selCurrSymb));
                            break;
                    }
                }
            }
            final StringBuilder js = new StringBuilder();

            for (final Entry<String, Set<String>> entry : map.entrySet()) {
                js.append("on(dom.byId(\"").append(entry.getKey()).append("\"), \"click\", function(evt){");
                for (final String key : entry.getValue()) {
                    js.append("dom.byId(\"").append(key)
                        .append("\").disabled = evt.currentTarget.checked ? '' : 'disabled';");
                }
                js.append("});\n");
            }
            js.append("  new ToggleButton({\n")
                .append("    showLabel: true,\n")
                .append("    label: 'activar todos',\n")
                .append("    checked: false,\n")
                .append("    onChange: function (val) {\n")
                .append("      this.set('label', val ? 'desactivar todos' : 'activar todos' );\n")
                .append("      query('input[type=checkbox]').forEach(function (node) {\n")
                .append("        node.checked = val;\n")
                .append("      });\n")
                .append("      query('[name^=\\'newValue_\\']').forEach(function (node) {\n")
                .append("        node.disabled = !val;\n")
                .append("      })\n")
                .append("    }\n")
                .append("  }, 'btn').startup();\n")
                .append("\n")
                .append("");

            final StringBuilder html = InterfaceUtils.wrappInScriptTag(_parameter,
                            InterfaceUtils.wrapInDojoRequire(_parameter, js, DojoLibs.ON, DojoLibs.DOM, DojoLibs.QUERY,
                                            DojoLibs.TOGGLEBUTTON), true, 1500);
            html.insert(0, table.toHtml());
            for (final String selected : selectedArr) {
                html.append("<input type=\"hidden\" name=\"selected\" value=\"")
                .append(selected).append("\">");
            }
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
        final String[] oids = _parameter.getParameterValues("oid");
        if (!ArrayUtils.isEmpty(oids)) {
            final String[] selectedArr = _parameter.getParameterValues("selected");
            final Map<String, String[]> valueMap = new HashMap<>();
            for (final String selected : selectedArr) {
                final String[] values = _parameter.getParameterValues("newValue_" + selected);
                if (values.length == oids.length) {
                    valueMap.put(selected, values);
                }
            }
            for (int i = 0; i < oids.length; i++) {
                final Instance docInst = Instance.get(oids[i]);
                final Map<Instance, BigDecimal> mapping = new HashMap<>();

                final PrintQuery print = new PrintQuery(docInst);
                final SelectBuilder selTemplInst = SelectBuilder.get().linkto(
                                CIPayroll.DocumentAbstract.TemplateLinkAbstract).instance();
                final SelectBuilder selRateCurInst = SelectBuilder.get().linkto(
                                CIPayroll.DocumentAbstract.RateCurrencyId).instance();
                print.addSelect(selTemplInst, selRateCurInst);
                print.addAttribute(CIPayroll.DocumentAbstract.Rate, CIPayroll.DocumentAbstract.Date,
                                CIPayroll.DocumentAbstract.LaborTime, CIPayroll.DocumentAbstract.HolidayLaborTime,
                                CIPayroll.DocumentAbstract.ExtraLaborTime, CIPayroll.DocumentAbstract.NightLaborTime);
                print.execute();
                final Instance rateCurrInst = print.getSelect(selRateCurInst);
                final Instance tmplInst = print.getSelect(selTemplInst);
                final Object[] rateObj = print.getAttribute(CIPayroll.DocumentAbstract.Rate);
                final Object[] laborTime = print.getAttribute(CIPayroll.DocumentAbstract.LaborTime);
                final Object[] extraLaborTime = print.getAttribute(CIPayroll.DocumentAbstract.ExtraLaborTime);
                final Object[] holidayLaborTime = print.getAttribute(CIPayroll.DocumentAbstract.HolidayLaborTime);
                final Object[] nightLaborTime = print.getAttribute(CIPayroll.DocumentAbstract.NightLaborTime);

                final Update update = new Update(docInst);
                for (final Entry<String, String[]> entry  :valueMap.entrySet()) {
                    switch (entry.getKey()) {
                        case "LaborTime":
                            update.add(CIPayroll.DocumentAbstract.LaborTime,
                                            new Object[] { entry.getValue()[i], laborTime[1] });
                            break;
                        case "ExtraLaborTime":
                            update.add(CIPayroll.DocumentAbstract.ExtraLaborTime,
                                            new Object[] { entry.getValue()[i], extraLaborTime[1] });
                            break;
                        case "HolidayLaborTime":
                            update.add(CIPayroll.DocumentAbstract.HolidayLaborTime,
                                            new Object[] { entry.getValue()[i], holidayLaborTime[1] });
                            break;
                        case "NightLaborTime":
                            update.add(CIPayroll.DocumentAbstract.NightLaborTime,
                                            new Object[] { entry.getValue()[i], nightLaborTime[1] });
                            break;
                        default:
                            try {
                                final PrintQuery keyPrint = new PrintQuery(entry.getKey());
                                keyPrint.addAttribute(CIPayroll.RuleAbstract.Key);
                                keyPrint.execute();
                                final String key = keyPrint.getAttribute(CIPayroll.RuleAbstract.Key);

                                final BigDecimal amount = (BigDecimal) NumberFormatter.get().getTwoDigitsFormatter()
                                    .parse(entry.getValue()[i]);

                                final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
                                queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.Key, key);
                                queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink, docInst);
                                final InstanceQuery query = queryBldr.getQuery();
                                query.execute();
                                if (query.next()) {
                                    mapping.put(query.getCurrentValue(), amount);
                                } else {
                                    final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Template2Rule);
                                    attrQueryBldr.addWhereAttrEqValue(CIPayroll.Template2Rule.FromLink, tmplInst);
                                    final QueryBuilder ruleQueryBldr = new QueryBuilder(CIPayroll.RuleInput);
                                    ruleQueryBldr.addWhereAttrInQuery(CIPayroll.RuleInput.ID,
                                                    attrQueryBldr.getAttributeQuery(CIPayroll.Template2Rule.ToLink));
                                    ruleQueryBldr.addWhereAttrEqValue(CIPayroll.RuleInput.Status,
                                                    Status.find(CIPayroll.RuleStatus.Active));
                                    ruleQueryBldr.addWhereAttrEqValue(CIPayroll.RuleInput.Key, key);
                                    final InstanceQuery ruleQuery = ruleQueryBldr.getQuery();
                                    ruleQuery.execute();
                                    if (ruleQuery.next()) {
                                        mapping.put(ruleQuery.getCurrentValue(), amount);
                                    }
                                }
                            } catch (final ParseException e) {
                                LOG.error("parser", e);
                            }
                            break;
                    }
                }
                update.add(CIPayroll.Advance.Basis, BasisAttribute.getValueList4Inst(_parameter, docInst));
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

    /**
     * Update totals.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
     * @param _result the result
     * @param _rateCurInst the rate cur inst
     * @param _rateObj the rate obj
     * @throws EFapsException on error
     */
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

    /**
     * Update positions.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
     * @param _result the result
     * @param _rateCurInst the rate cur inst
     * @param _rateObj the rate obj
     * @throws EFapsException on error
     */
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

}
