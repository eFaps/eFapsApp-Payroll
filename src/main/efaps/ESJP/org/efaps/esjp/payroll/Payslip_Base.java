/*
 * Copyright 2003 - 2010 The eFaps Team
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.engine.export.JRXlsExporter;

import org.apache.commons.lang.StringEscapeUtils;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.datamodel.ui.UIInterface;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsClassLoader;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.Rate;
import org.efaps.esjp.payroll.CasePosition_Base.MODE;
import org.efaps.esjp.ui.print.Table_Base;
import org.efaps.esjp.ui.print.Table_Base.Section;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import ar.com.fdvs.dj.core.DynamicJasperHelper;
import ar.com.fdvs.dj.core.layout.ClassicLayoutManager;
import ar.com.fdvs.dj.core.layout.ListLayoutManager;
import ar.com.fdvs.dj.domain.AutoText;
import ar.com.fdvs.dj.domain.ColumnOperation;
import ar.com.fdvs.dj.domain.DynamicReport;
import ar.com.fdvs.dj.domain.Style;
import ar.com.fdvs.dj.domain.builders.ColumnBuilder;
import ar.com.fdvs.dj.domain.builders.ColumnBuilderException;
import ar.com.fdvs.dj.domain.builders.DynamicReportBuilder;
import ar.com.fdvs.dj.domain.constants.Border;
import ar.com.fdvs.dj.domain.constants.Font;
import ar.com.fdvs.dj.domain.constants.Page;
import ar.com.fdvs.dj.domain.constants.Stretching;
import ar.com.fdvs.dj.domain.constants.Transparency;
import ar.com.fdvs.dj.domain.entities.columns.AbstractColumn;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f1c7d3d5-23d2-4d72-a5d4-3c811a159062")
@EFapsRevision("$Rev$")
public abstract class Payslip_Base
    extends CommonDocument
{

    /**
     * Classloader used for EventDefinition.
     */
    private static final EFapsClassLoader CLASSLOADER =  new EFapsClassLoader(Payslip.class.getClassLoader());

    /**
     * @return a formater used to format bigdecimal for the user interface
     * @param _maxFrac maximum Faction, null to deactivate
     * @param _minFrac minimum Faction, null to activate
     * @throws EFapsException on error
     */
    protected DecimalFormat getFormater(final Integer _minFrac,
                                        final Integer _maxFrac)
        throws EFapsException
    {
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        if (_maxFrac != null) {
            formater.setMaximumFractionDigits(_maxFrac);
        }
        if (_minFrac != null) {
            formater.setMinimumFractionDigits(_minFrac);
        }
        formater.setRoundingMode(RoundingMode.HALF_UP);
        formater.setParseBigDecimal(true);
        return formater;
    }

    /**
     * Method to set the instance of the payslip selected to the Context.
     *
     * @param _parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error
     */
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        if (Context.getThreadContext().getSessionAttribute("payslip") != null) {
            Context.getThreadContext().setSessionAttribute("payslip", null);
        }
        final String oid = _parameter.getParameterValue("selectedRow");
        final Instance inst = Instance.get(oid);
        Context.getThreadContext().setSessionAttribute("payslip", inst);
        final StringBuilder js = new StringBuilder();

        js.append("<script type=\"text/javascript\">");
        if (inst != null && inst.isValid()) {
            js.append(getSetValuesString(inst));
        }
        js.append("</script>");
        final Return ret = new Return();
        ret.put(ReturnValues.SNIPLETT, js.toString());
        return ret;
    }

    /**
     * Method to get the javascript part for setting the values.
     * @param _instance  instance to be copied
     * @return  javascript
     * @throws EFapsException on error
     */
    protected String getSetValuesString(final Instance _instance)
        throws EFapsException
    {

        final StringBuilder js = new StringBuilder();
        final PrintQuery print = new PrintQuery(_instance);
        print.addAttribute(CIPayroll.Payslip.Name,
                           CIPayroll.Payslip.Note,
                           CIPayroll.Payslip.LaborTime,
                           CIPayroll.Payslip.Amount2Pay,
                           CIPayroll.Payslip.AmountCost,
                           CIPayroll.Payslip.CurrencyLink);
        final SelectBuilder selEmpOID = new SelectBuilder().linkto(CIPayroll.Payslip.EmployeeAbstractLink).oid();
        final SelectBuilder selEmpNum = new SelectBuilder().linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                            .attribute(CIHumanResource.Employee.Number);
        final SelectBuilder selEmpLastName = new SelectBuilder().linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                            .attribute(CIHumanResource.Employee.LastName);
        final SelectBuilder selEmpFirstName = new SelectBuilder().linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                            .attribute(CIHumanResource.Employee.FirstName);
        print.addSelect(selEmpOID, selEmpLastName, selEmpFirstName, selEmpNum);
        print.execute();

        final BigDecimal amount2Pay = print.<BigDecimal>getAttribute(CIPayroll.Payslip.Amount2Pay);
        final BigDecimal amountCosts = print.<BigDecimal>getAttribute(CIPayroll.Payslip.AmountCost);
        final String empOid = print.<String>getSelect(selEmpOID);
        final String empNum = print.<String>getSelect(selEmpNum);
        final String empLName = print.<String>getSelect(selEmpLastName);
        final String empFName = print.<String>getSelect(selEmpFirstName);
        final Object[] laborTime = print.getAttribute(CIPayroll.Payslip.LaborTime);
        final BigDecimal laborTimeVal = (BigDecimal) laborTime[0];
        final UoM laborTimeUoM = (UoM) laborTime[1];

        final Long curId = print.<Long>getAttribute(CIPayroll.Payslip.CurrencyLink);


        final DecimalFormat formater = getTwoDigitsformater();

        js.append("function removeRows(elName){")
            .append("e = document.getElementsByName(elName);")
            .append("zz = e.length;")
            .append("for (var i=0; i <zz;i++) {")
            .append("x = e[0].parentNode.parentNode;")
            .append("var p = x.parentNode;p.removeChild(x);")
            .append("}}")
            .append("function setValue() {")
            .append("document.getElementsByName('currencyLink')[0].value=").append(curId).append(";")
            .append(getSetFieldValue(0, "number", empOid))
            .append(getSetFieldValue(0, "numberAutoComplete", empNum))
            .append(getSetFieldValue(0, "employeeData", empLName + ", " + empFName))
            .append(getSetFieldValue(0, "laborTime", formater.format(laborTimeVal)))
            .append("document.getElementsByName('laborTimeUoM')[0].value=").append(laborTimeUoM.getId()).append(";")
            .append(getSetFieldValue(0, "amount2Pay", amount2Pay == null
                            ? BigDecimal.ZERO.toString() : formater.format(amount2Pay)))
            .append(getSetFieldValue(0, "amountCost", amountCosts == null
                            ? BigDecimal.ZERO.toString() : formater.format(amountCosts)))
            .append("}");

        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink, _instance.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.PositionAbstract.PositionNumber,
                            CIPayroll.PositionAbstract.Amount,
                            CIPayroll.PositionAbstract.Description);
        final SelectBuilder selCaseOID = new SelectBuilder()
                            .linkto(CIPayroll.PositionAbstract.CasePositionAbstractLink).oid();
        final SelectBuilder selCaseName = new SelectBuilder()
                            .linkto(CIPayroll.PositionAbstract.CasePositionAbstractLink)
                            .attribute(CIPayroll.CasePositionCalc.Name);
        multi.addSelect(selCaseOID, selCaseName);
        multi.execute();

        final Map<Integer, Object[]> values = new TreeMap<Integer, Object[]>();

        while (multi.next()) {
            final BigDecimal amount = multi.<BigDecimal>getAttribute(CIPayroll.PositionAbstract.Amount);

            final Object[] value = new Object[] { multi.getSelect(selCaseName),
                            multi.getSelect(selCaseOID),
                            multi.getAttribute(CIPayroll.PositionAbstract.Description),
                            amount,
                            multi.getCurrentInstance()};
            values.put(multi.<Integer>getAttribute(CIPayroll.PositionAbstract.PositionNumber), value);
        }

        final StringBuilder dedBldr = new StringBuilder();
        final StringBuilder payBldr = new StringBuilder();
        final StringBuilder neuBldr = new StringBuilder();
        dedBldr.append("function setDeduction(){");
        payBldr.append("function setPayment(){");
        neuBldr.append("function setNeutral(){");
        int ded = 0;
        int pay = 0;
        int neu = 0;
        if (!values.isEmpty()) {
            for (final Entry<Integer, Object[]> entry : values.entrySet()) {
                final Instance instPos = (Instance) entry.getValue()[4];
                if (instPos.getType().isKindOf(Type.get(CIPayroll.PositionPayment.uuid))) {
                    payBldr.append(getSetFieldValue(pay, "casePosition_PaymentAutoComplete",
                                    (String) entry.getValue()[0]))
                        .append(getSetFieldValue(pay, "casePosition_Payment", (String) entry.getValue()[1]))
                        .append(getSetFieldValue(pay, "description_Payment", (String) entry.getValue()[2]))
                        .append(getSetFieldValue(pay, "amount_Payment", formater.format(entry.getValue()[3])));
                    pay++;
                } else if (instPos.getType().isKindOf(Type.get(CIPayroll.PositionDeduction.uuid))) {
                    dedBldr.append(getSetFieldValue(ded, "casePosition_DeductionAutoComplete",
                                    (String) entry.getValue()[0]))
                        .append(getSetFieldValue(ded, "casePosition_Deduction", (String) entry.getValue()[1]))
                        .append(getSetFieldValue(ded, "description_Deduction", (String) entry.getValue()[2]))
                        .append(getSetFieldValue(ded, "amount_Deduction", formater.format(entry.getValue()[3])));
                    ded++;
                } else if (instPos.getType().isKindOf(Type.get(CIPayroll.PositionNeutral.uuid))) {
                    neuBldr.append(getSetFieldValue(neu, "casePosition_NeutralAutoComplete",
                                    (String) entry.getValue()[0]))
                        .append(getSetFieldValue(neu, "casePosition_Neutral", (String) entry.getValue()[1]))
                        .append(getSetFieldValue(neu, "description_Neutral", (String) entry.getValue()[2]))
                        .append(getSetFieldValue(neu, "amount_Neutral", formater.format(entry.getValue()[3])));
                    neu++;
                }
            }
        }
        payBldr.append("}");
        dedBldr.append("}");
        neuBldr.append("}");
        js.append(payBldr).append(dedBldr).append(neuBldr)
            .append("Wicket.Event.add(window, \"domready\", function(event) {")
            .append("removeRows('amount_Payment');")
            .append("removeRows('amount_Deduction');")
            .append("removeRows('amount_Neutral');")
            .append("addNewRows_paymentTable(").append(pay).append(", setPayment, null);")
            .append("addNewRows_deductionTable(").append(ded).append(", setDeduction, null);")
            .append("addNewRows_neutralTable(").append(neu).append(", setNeutral, null);")
            .append("setValue();")
            .append(" });");

        return js.toString();
    }

    /**
     * Method to get a formatter for two decimals.
     *
     * @return DecimalFormat with the format.
     * @throws EFapsException on error.
     */
    protected DecimalFormat getTwoDigitsformater()
        throws EFapsException
    {
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        formater.setMaximumFractionDigits(2);
        formater.setMinimumFractionDigits(2);
        formater.setRoundingMode(RoundingMode.HALF_UP);
        formater.setParseBigDecimal(true);
        return formater;
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

        final String[] employees = _parameter.getParameterValues("employee");
        final String[] employeeNames = _parameter.getParameterValues("employeeAutoComplete");
        final String[] laborTimes = _parameter.getParameterValues("laborTime");
        final String[] laborTimeUoMs = _parameter.getParameterValues("laborTimeUoM");
        final String[] amount2Pays = _parameter.getParameterValues("amount2Pay");
        final String[] amountCosts = _parameter.getParameterValues("amountCost");
        final String[] currencyLinks = _parameter.getParameterValues("currencyLink");
        final DecimalFormat formater = getFormater(2, 2);
        try {
            for (int i = 0; i < employees.length; i++) {
                if (employees[i] != null && !employees[i].isEmpty()
                                && laborTimes[i] != null && !laborTimes[i].isEmpty()) {
                    final BigDecimal pay = amount2Pays[i] != null && !amount2Pays[i].isEmpty()
                                                    ? (BigDecimal) formater.parse(amount2Pays[i])
                                                    : BigDecimal.ZERO;
                    final BigDecimal cost = amountCosts[i] != null && !amountCosts[i].isEmpty()
                                                    ? (BigDecimal) formater.parse(amountCosts[i])
                                                    : BigDecimal.ZERO;
                    final BigDecimal time = laborTimes[i] != null && !laborTimes[i].isEmpty()
                                                    ? (BigDecimal) formater.parse(laborTimes[i])
                                                    : BigDecimal.ZERO;
                    final Insert insert = new Insert(CIPayroll.Payslip);
                    insert.add(CIPayroll.Payslip.Name, name + " " + employeeNames[i]);
                    insert.add(CIPayroll.Payslip.Date, date);
                    insert.add(CIPayroll.Payslip.DueDate, dueDate);
                    insert.add(CIPayroll.Payslip.EmployeeAbstractLink, Instance.get(employees[i]).getId());
                    insert.add(CIPayroll.Payslip.StatusAbstract,
                                    ((Long) Status.find(CIPayroll.PayslipStatus.uuid, "Open").getId()).toString());
                    insert.add(CIPayroll.Payslip.Amount2Pay, pay);
                    insert.add(CIPayroll.Payslip.AmountCost, cost);
                    insert.add(CIPayroll.Payslip.CurrencyLink, currencyLinks[i]);
                    insert.add(CIPayroll.Payslip.LaborTime, new Object[] { time, laborTimeUoMs[i]});
                    insert.execute();
                }
            }
        } catch (final ParseException e) {
            throw new EFapsException(Payslip_Base.class, "createMassive.ParseException", e);
        }
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
        Return ret = new Return();
        final String name = getDocName4Create(_parameter);
        final String date = _parameter.getParameterValue("date");
        final String dueDate = _parameter.getParameterValue("dueDate");
        final Long employeeid = Instance.get(_parameter.getParameterValue("number")).getId();
        final String laborTimes = _parameter.getParameterValue("laborTime");
        final String laborTimeUoMs = _parameter.getParameterValue("laborTimeUoM");
        final String amount2Pay = _parameter.getParameterValue("amount2Pay");
        final String amountCosts = _parameter.getParameterValue("amountCost");
        final String currencyLinks = _parameter.getParameterValue("currencyLink");
        final DecimalFormat formater = getFormater(2, 2);
        try {
            final Insert insert = new Insert(CIPayroll.Payslip);
            insert.add(CIPayroll.Payslip.Name, name);
            insert.add(CIPayroll.Payslip.Date, date);
            insert.add(CIPayroll.Payslip.DueDate, dueDate);
            insert.add(CIPayroll.Payslip.EmployeeAbstractLink, employeeid);
            insert.add(CIPayroll.Payslip.StatusAbstract,
                            ((Long) Status.find(CIPayroll.PayslipStatus.uuid, "Open").getId()).toString());
            final BigDecimal pay = amount2Pay != null && !amount2Pay.isEmpty()
                                                        ? (BigDecimal) formater.parse(amount2Pay)
                                                        : BigDecimal.ZERO;
            final BigDecimal cost = amountCosts != null && !amountCosts.isEmpty()
                                                        ? (BigDecimal) formater.parse(amountCosts)
                                                        : BigDecimal.ZERO;
            final BigDecimal time = laborTimes != null && !laborTimes.isEmpty()
                                                        ? (BigDecimal) formater.parse(laborTimes)
                                                        : BigDecimal.ZERO;
            insert.add(CIPayroll.Payslip.Amount2Pay, pay);
            insert.add(CIPayroll.Payslip.AmountCost, cost);
            insert.add(CIPayroll.Payslip.CurrencyLink, currencyLinks);
            insert.add(CIPayroll.Payslip.LaborTime, new Object[] { time, laborTimeUoMs});
            insert.execute();
            final Map<Instance, TablePos> values = new HashMap<Instance, TablePos>();
            analyseTables(_parameter, values, "Deduction");
            analyseTables(_parameter, values, "Payment");
            analyseTables(_parameter, values, "Neutral");

            final Map<Instance, SumPosition> sums = analyseSums(_parameter);

            final List<InsertPos> list = new ArrayList<InsertPos>();
            for (final SumPosition pos : sums.values()) {
                list.add(new InsertPos(pos, _parameter, sums, values));
            }
            for (final Entry<Instance, TablePos> entry : values.entrySet()) {
                for (final BigDecimal amount : entry.getValue().getValues()) {
                    list.add(new InsertPos(entry.getKey(), amount));
                }
            }
            Collections.sort(list, new Comparator<InsertPos>() {
                @Override
                public int compare(final InsertPos _insertPos1,
                                   final InsertPos _insertPos2)
                {
                    return _insertPos1.getSorted().compareTo(_insertPos2.getSorted());
                }
            });
            // Payroll-Configuration
            final SystemConfiguration config = SystemConfiguration.get(
                            UUID.fromString("6f21b777-3c7d-4792-b3c0-8bfb6af0bf5e"));
            final Instance currInst = config.getLink("Currency4Payslip");

            if (!currInst.isValid()) {
                throw new EFapsException(Payslip_Base.class, "create.MissingConfigLink");
            }
            int i = 0;

            final boolean active = config.getAttributeValueAsBoolean("ActivateAccountingTransaction");
            if (active) {
                createTransaction(_parameter, list);
            }

            BigDecimal sumDeduction = BigDecimal.ZERO;
            BigDecimal sumPayment = BigDecimal.ZERO;
            BigDecimal sumNeutral = BigDecimal.ZERO;
            for (final InsertPos pos : list) {
                final Insert insertPos = new Insert(pos.getType());
                if (pos.getType().equals(CIPayroll.PositionDeduction.getType())) {
                    sumDeduction  = sumDeduction.add(pos.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
                } else if (pos.getType().equals(CIPayroll.PositionNeutral.getType())) {
                    sumNeutral  = sumNeutral.add(pos.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
                } else if (pos.getType().equals(CIPayroll.PositionPayment.getType())) {
                    sumPayment  = sumPayment.add(pos.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
                }
                insertPos.add(CIPayroll.PositionAbstract.Amount, pos.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
                insertPos.add(CIPayroll.PositionAbstract.CasePositionAbstractLink, pos.getId());
                insertPos.add(CIPayroll.PositionAbstract.CurrencyLink, currInst.getId());
                insertPos.add(CIPayroll.PositionAbstract.Description, pos.getDescription());
                insertPos.add(CIPayroll.PositionAbstract.DocumentAbstractLink, insert.getInstance().getId());
                insertPos.add(CIPayroll.PositionAbstract.PositionNumber, i);
                insertPos.execute();
                i++;
            }
            if (amount2Pay == null && amountCosts == null) {
                setAmounts(_parameter, insert.getInstance(), list);
            }

            _parameter.put(ParameterValues.INSTANCE, insert.getInstance());
            final StandartReport report = new StandartReport();
            final String fileName = CIPayroll.Payslip.getType().getName() + "_" + name;
            report.setFileName(fileName);
            report.getJrParameters().put("sumDeduction", sumDeduction);
            report.getJrParameters().put("sumPayment", sumPayment);
            report.getJrParameters().put("sumNeutral", sumNeutral);
            report.getJrParameters().put("sumTotal", sumPayment.subtract(sumDeduction));

            ret = report.execute(_parameter);

        } catch (final ParseException e) {
            throw new EFapsException(Payslip_Base.class, "create.ParseException", e);
        }

        return ret;
    }

    /**
     * @param _parameter as passed from eFaps API.
     * @param lstPos
     * @return
     * @throws EFapsException on error.
     */
    private Return createTransaction (final Parameter _parameter,
                                      final List<InsertPos> lstPos) throws EFapsException {
        final Instance instEmployee = Instance.get(_parameter.getParameterValue("number"));
        // Accounting_Periode
        final QueryBuilder queryBlrd = new QueryBuilder(UUID.fromString("a8ac11ce-feb9-4ba6-bffd-8174906190f5"));
        queryBlrd.addWhereAttrGreaterValue("ToDate", new DateTime().minusDays(1));
        queryBlrd.addWhereAttrLessValue("FromDate", new DateTime().plusSeconds(1));
        final MultiPrintQuery multi = queryBlrd.getPrint();
        multi.execute();
        if (multi.next() && instEmployee.isValid()) {
            final Instance instPer = multi.getCurrentInstance();

            final PrintQuery printEmp = new PrintQuery(instEmployee);
            printEmp.addAttribute(CIHumanResource.Employee.LastName, CIHumanResource.Employee.FirstName);
            printEmp.execute();

            final String employee = printEmp.<String>getAttribute(CIHumanResource.Employee.LastName)
                            + ", " + printEmp.<String>getAttribute(CIHumanResource.Employee.FirstName);

            // Accounting_Transaction
            final Insert insert = new Insert(UUID.fromString("f1962037-c73b-4b55-94d8-f33e47a219d6"));
            insert.add("Name", _parameter.getParameterValue("name"));
            insert.add("Description", _parameter.getParameterValue("name") + " - " + employee);
            insert.add("Date", _parameter.getParameterValue("transactionDate"));
            insert.add("PeriodeLink", instPer.getId());
            // Accounting_TransactionStatus
            insert.add("Status", Status.find(UUID.fromString("bdd988b5-9e97-4633-9dae-fde41c671d3e"), "Open").getId());
            insert.execute();

            for (final InsertPos pos : lstPos) {
                if (pos.getType().equals(CIPayroll.PositionDeduction.getType())
                                || pos.getType().equals(CIPayroll.PositionNeutral.getType())
                                || pos.getType().equals(CIPayroll.PositionPayment.getType())) {
                    final PrintQuery print = new PrintQuery(pos.caseType, pos.getId());
                    final SelectBuilder selActionOid = new SelectBuilder().linkto("ActionDefinitionLink").oid();
                    print.addSelect(selActionOid);
                    print.execute();
                    final Instance instAction = Instance.get(print.<String>getSelect(selActionOid));
                    if (instAction.isValid()) {
                        insertPositions(_parameter, insert.getInstance(), instAction, instPer, pos, "Payment");
                    }
                }
            }
        }

        return new Return();
    }

    /**
     * @param _parameter
     * @param _instance
     * @param _instAction
     * @param _instPeriode
     * @param _pos
     * @param _postFix
     * @throws EFapsException
     */
    public void insertPositions(final Parameter _parameter,
                                final Instance _instance,
                                final Instance _instAction,
                                final Instance _instPeriode,
                                final InsertPos _pos,
                                final String _postFix)
        throws EFapsException
    {
        // Accounting_CasePayroll
        final QueryBuilder attrQueryBldr = new QueryBuilder(UUID.fromString("68f1faef-6779-498d-b37d-bc2bd5e9a453"));
        attrQueryBldr.addWhereAttrEqValue("PeriodeAbstractLink", _instPeriode.getId());
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery("ID");

        // Accounting_ActionDefinition2Case
        final QueryBuilder attrQueryBldr2 = new QueryBuilder(UUID.fromString("5b4f8626-c8f8-44b5-823d-1ba5817631d1"));
        attrQueryBldr2.addWhereAttrEqValue("FromLink", _instAction.getId());
        attrQueryBldr2.addWhereAttrInQuery("ToLink", attrQuery);
        final AttributeQuery attrQuery2 = attrQueryBldr2.getAttributeQuery("ToLink");

        // Accounting_Account2CaseAbstract
        final QueryBuilder queryBldr = new QueryBuilder(UUID.fromString("7efe5f1a-4fdc-41d7-8f55-77777212f02b"));
        queryBldr.addWhereAttrInQuery("ToCaseAbstractLink", attrQuery2);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selAccountOid = new SelectBuilder().linkto("FromAccountAbstractLink").oid();
        multi.addSelect(selAccountOid);
        multi.execute();
        while (multi.next()) {
            final String accountOid = multi.<String>getSelect(selAccountOid);
            Type type = null;
            // Accounting_Account2CaseCredit
            if (multi.getCurrentInstance().getType()
                            .isKindOf(Type.get(UUID.fromString("f6f52802-c362-4e77-9068-ba02303f7b5c")))) {
                // Accounting_TransactionPositionCredit
                type = Type.get(UUID.fromString("5336550c-3da0-41da-9b94-0f68788423b1"));
            // Accounting_Account2CaseDebit
            } else if (multi.getCurrentInstance().getType()
                            .isKindOf(Type.get(UUID.fromString("c413ba55-714a-415d-b904-9af49efeb785")))) {
                // Accounting_TransactionPositionDebit
                type = Type.get(UUID.fromString("2190cf3c-eb36-4565-9d8a-cd9105451f35"));
            }

            // Payroll-Configuration
            final SystemConfiguration config = SystemConfiguration.get(
                            UUID.fromString("6f21b777-3c7d-4792-b3c0-8bfb6af0bf5e"));
            final Instance currInst = config.getLink("Currency4Payslip");

            if (!currInst.isValid()) {
                throw new EFapsException(Payslip_Base.class, "create.MissingConfigLink");
            }

            //final Object[] rateObj = new Object[] {BigDecimal.ONE, BigDecimal.ONE};
            final Object[] rateObj = getRateObject(_instPeriode, currInst);
            final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                            BigDecimal.ROUND_HALF_UP);
            final Insert insert2 = new Insert(type);
            insert2.add("TransactionLink", _instance.getId());
            insert2.add("AccountLink", Instance.get(accountOid).getId());
            insert2.add("CurrencyLink", currInst.getId());
            insert2.add("RateCurrencyLink", currInst.getId());
            insert2.add("Rate", rateObj);

            final BigDecimal rateAmount = _pos.getAmount().setScale(6, BigDecimal.ROUND_HALF_UP);
            final boolean isDebitTrans = type.getUUID().equals(UUID.fromString("2190cf3c-eb36-4565-9d8a-cd9105451f35"));
            insert2.add("RateAmount", isDebitTrans ? rateAmount.negate() : rateAmount);
            final BigDecimal amount = rateAmount.divide(rate, 12, BigDecimal.ROUND_HALF_UP);
            insert2.add("Amount", isDebitTrans ? amount.negate() : amount);
            insert2.execute();

        }

    }

    /**
     * Get the exchange Rate for a Date inside a periode.
     *
     * @param _periodeInst  Instance of the periode
     * @param _currId       id of the currency
     * @param _date         date the exchange rate is wanted for
     * @return rate
     * @throws EFapsException on error
     */
    protected Rate getExchangeRate(final Instance _periodeInst,
                                   final Long _currId,
                                   final DateTime _date)
        throws EFapsException
    {

        final Rate ret;

        final PrintQuery print = new PrintQuery(_periodeInst);
        final SelectBuilder selCur = new SelectBuilder().linkto("CurrencyLink").oid();
        print.addSelect(selCur);
        print.execute();
        final CurrencyInst curInstance = new CurrencyInst(Instance.get(print.<String> getSelect(selCur)));
        if (curInstance.getInstance().getId() == _currId) {
            ret = new Rate(curInstance, BigDecimal.ONE);
        } else {
            // Accounting_ERP_CurrencyRateAccounting
            final QueryBuilder queryBldr = new QueryBuilder(UUID.fromString("3fecbf3a-4454-40ef-a182-713b211a283f"));
            queryBldr.addWhereAttrEqValue("CurrencyLink", _currId);
            queryBldr.addWhereAttrLessValue("ValidFrom",
                            _date.plusMinutes(1));
            queryBldr.addWhereAttrGreaterValue("ValidUntil",
                            _date.minusMinutes(1));
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder valSel = new SelectBuilder()
                            .attribute("Rate").value();
            final SelectBuilder labSel = new SelectBuilder()
                            .attribute("Rate").label();
            final SelectBuilder curSel = new SelectBuilder()
                            .linkto("CurrencyLink").oid();
            multi.addSelect(valSel, labSel, curSel);
            multi.execute();
            if (multi.next()) {
                ret = new Rate(new CurrencyInst(Instance.get(multi.<String>getSelect(curSel))),
                                multi.<BigDecimal>getSelect(valSel),
                                multi.<BigDecimal>getSelect(labSel));
            } else {
                ret = new Rate(new CurrencyInst(Instance.get(CIERP.Currency.getType(), _currId)), BigDecimal.ONE);
            }
        }

        return ret;
    }

    /**
     * @param _periodeInstance
     * @param _currInstance
     * @return
     * @throws EFapsException
     */
    protected Object[] getRateObject(final Instance _periodeInstance,
                                     final Instance _currInstance)
        throws EFapsException
    {
        final Rate rate = getExchangeRate(_periodeInstance, _currInstance.getId(), new DateTime());
        return new Object[] { BigDecimal.ONE, rate.getLabel() };
    }


    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _docInst      instance of the document to be updated
     * @param _list         list of positions
     * @throws EFapsException on error
     */
    protected void setAmounts(final Parameter _parameter,
                              final Instance _docInst,
                              final List<InsertPos> _list)
          throws EFapsException
    {
        BigDecimal amount2pay = BigDecimal.ZERO;
        BigDecimal amountCost = BigDecimal.ZERO;
        for (final InsertPos pos : _list) {
            if (pos.caseType.equals(CIPayroll.CasePositionRootSum.getType())) {
                amount2pay = amount2pay.add(pos.getAmount());
            } else if (pos.posType.equals(CIPayroll.PositionNeutral.getType())
                            || pos.posType.equals(CIPayroll.PositionPayment.getType())) {
                amountCost = amountCost.add(pos.getAmount());
            }
        }
        final Update update = new Update(_docInst);
        update.add(CIPayroll.Payslip.Amount2Pay, amount2pay);
        update.add(CIPayroll.Payslip.AmountCost, amountCost);
        update.execute();
    }

    /**
     * Get the name for the document on creation.
     * @param _parameter    Parameter as passed by the eFaps API
     * @return new Name
     * @throws EFapsException on error
     */
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        return _parameter.getParameterValue("name");
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
        print.execute();
        final String firstname = print.<String>getAttribute(CIHumanResource.EmployeeAbstract.FirstName);
        final String lastname = print.<String>getAttribute(CIHumanResource.EmployeeAbstract.LastName);

        final StringBuilder strBldr = new StringBuilder();
        strBldr.append(lastname).append(", ").append(firstname);
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
        final Instance instance = Instance.get(_parameter.getParameterValue("number"));
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        if (instance.getId() > 0) {
            map.put("employeeData", getFieldValue4Employee(instance));
        } else {
            map.put("employeeData", "????");
        }
        list.add(map);
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }


    /**
     * Executed as update event form the case field.
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return update4Case(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String caseOid = _parameter.getParameterValue("case");
        Context.getThreadContext().setSessionAttribute(Case_Base.CASE_SESSIONKEY, caseOid);
        return ret;
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
        final String caseOid = _parameter.getParameterValue("case");
        final Instance caseInst = Instance.get(caseOid);
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.CasePositionCalc);
        queryBldr.addWhereAttrEqValue(CIPayroll.CasePositionCalc.CaseAbstractLink, caseInst.getId());
        queryBldr.addWhereAttrNotEqValue(CIPayroll.CasePositionCalc.Mode, CasePosition.MODE.DEAVTIVATED.ordinal(),
                                                                          CasePosition.MODE.OPTIONAL.ordinal());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.CasePositionCalc.Name,
                           CIPayroll.CasePositionCalc.Description,
                           CIPayroll.CasePositionCalc.Mode,
                           CIPayroll.CasePositionCalc.DefaultValue);
        multi.execute();

        final Map<String, Set<Object[]>> values = new TreeMap<String, Set<Object[]>>();
        while (multi.next()) {
            final String name = multi.<String> getAttribute(CIPayroll.CasePositionCalc.Name);
            final String desc = multi.<String> getAttribute(CIPayroll.CasePositionCalc.Description);
            final Integer mode = multi.<Integer> getAttribute(CIPayroll.CasePositionCalc.Mode);
            final BigDecimal dflt = multi.<BigDecimal> getAttribute(CIPayroll.CasePositionCalc.DefaultValue);

            final Instance instance = multi.getCurrentInstance();
            final String oid = instance.getOid();
            final String postFix;
            if (instance.getType().equals(CIPayroll.CasePositionDeduction.getType())) {
                postFix = "_Deduction";
            } else if (instance.getType().equals(CIPayroll.CasePositionPayment.getType())) {
                postFix = "_Payment";
            } else {
                postFix = "_Neutral";
            }
            final Object[] value = new Object[] { oid, name, desc, postFix, mode, dflt};
            Set<Object[]> set;
            if (values.containsKey(name)) {
                set = values.get(name);
            } else {
                set = new HashSet<Object[]>();
            }
            set.add(value);
            values.put(name, set);
        }
        final StringBuilder js = new StringBuilder();

        js.append("function removeRows(elName){")
            .append("e = document.getElementsByName(elName);")
            .append("zz = e.length;")
            .append("for (var i=0; i <zz;i++) {")
            .append("x = e[0].parentNode.parentNode;")
            .append("var p = x.parentNode;p.removeChild(x);")
            .append("}}")
            .append("removeRows('amount_Payment');")
            .append("removeRows('amount_Deduction');")
            .append("removeRows('amount_Neutral');");
        final StringBuilder dedBldr = new StringBuilder();
        final StringBuilder payBldr = new StringBuilder();
        final StringBuilder neuBldr = new StringBuilder();
        dedBldr.append("function setDeduction(){");
        payBldr.append("function setPayment(){");
        neuBldr.append("function setNeutral(){");
        int deb = 0;
        int cred = 0;
        int neu = 0;
        final DecimalFormat formater = getFormater(2, 2);
        for (final Set<Object[]> set : values.values()) {
            for (final Object[] value : set) {
                final int count;
                final StringBuilder bldr;
                if ("_Deduction".equals(value[3])) {
                    bldr = dedBldr;
                    count = deb;
                    deb++;
                } else if ("_Payment".equals(value[3])) {
                    bldr = payBldr;
                    count = cred;
                    cred++;
                } else {
                    bldr = neuBldr;
                    count = neu;
                    neu++;
                }
                bldr.append("document.getElementsByName('casePosition").append(value[3])
                    .append("AutoComplete')[").append(count)
                    .append("].value='").append(StringEscapeUtils.escapeJavaScript((String) value[1])).append("';")
                    .append("document.getElementsByName('casePosition").append(value[3])
                    .append("')[").append(count).append("].value='")
                    .append(value[0]).append("';")
                    .append("document.getElementsByName('description").append(value[3])
                    .append("')[").append(count).append("].appendChild(document.createTextNode('")
                    .append(StringEscapeUtils.escapeJavaScript((String) value[2])).append("'));");
                if (!value[4].equals(MODE.OPTIONAL_DEFAULT.ordinal())) {
                    bldr .append("x = document.getElementsByName('casePosition").append(value[3])
                        .append("AutoComplete')[").append(count).append("];")
                        .append("x.disabled = true;")
                        .append("var y = x.parentNode.parentNode.firstChild;")
                        .append("while (y.nodeName != 'DIV') {")
                        .append("y = y.nextSibling;")
                        .append("}")
                        .append("y.innerHTML='';");
                }
                if (value[4].equals(MODE.REQUIRED_NOEDITABLE.ordinal())) {
                    bldr .append("x = document.getElementsByName('amount").append(value[3]).append("')[")
                        .append(count).append("];")
                        .append("x.readOnly = true;");
                }
                if (value[5] != null) {
                    bldr.append("document.getElementsByName('amount").append(value[3])
                        .append("')[").append(count).append("].value='")
                        .append(formater.format(value[5])).append("';");
                }
            }
        }
        dedBldr.append("}");
        payBldr.append("}");
        neuBldr.append("}");

        js.append(dedBldr).append(payBldr).append(neuBldr)
            .append(" addNewRows_paymentTable(").append(cred)
            .append(", setPayment, null);")
            .append(" addNewRows_deductionTable(").append(deb)
            .append(", setDeduction, null);")
            .append(" addNewRows_neutralTable(").append(neu)
            .append(", setNeutral, null);");

        ret.put(ReturnValues.SNIPLETT, js.toString());
        return ret;
    }

    /**
     * @param _parameter as passed from eFaps API.
     * @return
     * @throws EFapsException on error.
     */
    protected Map<Instance, SumPosition> analyseSums(final Parameter _parameter)
        throws EFapsException
    {
        final Map<Instance, SumPosition> ret = new HashMap<Instance, SumPosition>();

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
     * @param _parameter as passed from eFaps API.
     * @return
     * @throws EFapsException on error.
     */
    public Return rateCurrencyFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final QueryBuilder queryBldr = new QueryBuilder(CIERP.Currency);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIERP.Currency.Name);
        multi.execute();
        final Map<String, Long> values = new TreeMap<String, Long>();
        while (multi.next()) {
            values.put(multi.<String>getAttribute(CIERP.Currency.Name), multi.getCurrentInstance().getId());
        }
        // Sales-Configuration
        final Instance baseInst = SystemConfiguration.get(UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"))
                        .getLink("CurrencyBase");
        final StringBuilder html = new StringBuilder();
        html.append("<select ").append(UIInterface.EFAPSTMPTAG)
                        .append(" name=\"").append(fieldValue.getField().getName()).append("\" size=\"1\">");
        for (final Entry<String, Long> entry : values.entrySet()) {
            html.append("<option value=\"").append(entry.getValue()).append("\"");
            if (entry.getValue().equals(baseInst.getId())) {
                html.append(" selected=\"selected\" ");
            }
            html.append(">").append(entry.getKey()).append("</option>");
        }
        html.append("</select>");
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, html.toString());
        return retVal;
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


        final Map<Instance, TablePos> values = new HashMap<Instance, TablePos>();
        analyseTables(_parameter, values, "Deduction");
        analyseTables(_parameter, values, "Payment");
        analyseTables(_parameter, values, "Neutral");

        final Map<Instance, SumPosition> sums = analyseSums(_parameter);

        final List<SumPosition> sort = new ArrayList<SumPosition>();
        sort.addAll(sums.values());
        Collections.sort(sort, new Comparator<SumPosition>() {
            @Override
            public int compare(final SumPosition _pos1,
                               final SumPosition _pos2)
            {
                return _pos1.getSorted().compareTo(_pos2.getSorted());
            }
        });
        final DecimalFormat formater = getFormater(2, 2);
        final StringBuilder html = new StringBuilder();
        html.append("document.getElementsByName('sums')[0].innerHTML='<table>");
        for (final SumPosition pos : sort) {
            if (pos.getMode() != CasePosition_Base.MODE.DEAVTIVATED.ordinal()) {
                html.append("<tr><td>")
                    .append(pos.getName()).append("</td><td>")
                    .append(StringEscapeUtils.escapeJavaScript(pos.getDescription())).append("</td><td>")
                    .append(formater.format(pos.getResult(_parameter, sums, values))).append("</td></tr>");
            }
        }
        html.append("</table>';");

        for (final TablePos pos : values.values()) {
            if (pos.isSetValue()) {
                for (int i = 0; i < pos.getValues().size(); i++) {
                    html.append("document.getElementsByName('amount_").append(pos.postfix).append("')[")
                        .append(pos.pos.get(i)).append("].value='").append(formater.format(pos.getValues().get(i)))
                        .append("';");
                }
            }
        }

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), html.toString());
        list.add(map);
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * Get the values from the tables.
     * @param _parameter    Parameter as passed from the eFaps API
     * @param _values
     * @param _postfix
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
            final DecimalFormat formater = getFormater(2, 2);
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
        final Return ret = new Return();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);

        final String mime = (String) properties.get("Mime");
        final boolean print = "pdf".equalsIgnoreCase(mime);

        final Style detailStyle = getStyle(_parameter, Table_Base.Section.DETAIL);
        final Style headerStyle = getStyle(_parameter, Table_Base.Section.HEADER);
        final Style titleStyle = getStyle(_parameter, Table_Base.Section.TITLE);
        final Style subtitleStyle = getStyle(_parameter, Table_Base.Section.SUBTITLE);
        final Style columnStyle = getStyle(_parameter, Table_Base.Section.COLUMN);
        final Style columnHeaderStyle = getStyle(_parameter, Table_Base.Section.COLUMNHEADER);

        final DynamicReportBuilder drb = new DynamicReportBuilder()
            .setTitle("ActionReport")
            .setUseFullPageWidth(true);
        if (print) {
            drb.setDetailHeight(15)
                .setHeaderHeight(15)
                .setDefaultStyles(titleStyle, subtitleStyle, headerStyle, detailStyle)
                .setColumnsPerPage(1)
                .setPageSizeAndOrientation(Page.Page_A4_Landscape())
                .setMargins(20, 20, 20, 20); // (top, bottom, left and right)
        } else {
            drb.setPrintColumnNames(true)
                .setIgnorePagination(true)
                .setMargins(0, 0, 0, 0)
                .setReportName("ActionReport")
                .setDefaultStyles(titleStyle, subtitleStyle, headerStyle, detailStyle);
        }

        try {
            final Map<String, Boolean> columns = getActionColums();

            ColumnBuilder cbldr = ColumnBuilder.getNew();
            cbldr.setColumnProperty("employee", String.class.getName()).setTitle("RR.HH.");
            drb.addColumn(cbldr.build());

            final List<AbstractColumn> lstColumsSum = new ArrayList<AbstractColumn>();
            for (final Entry<String, Boolean> entry : columns.entrySet()) {
                cbldr = ColumnBuilder.getNew();
                final String clazzname = BigDecimal.class.getName();
                final String[] nameOid = entry.getKey().split(":");
                cbldr.setColumnProperty(nameOid[0], clazzname).setTitle(nameOid[1]);
                if (print) {
                    cbldr.setStyle(columnStyle)
                        .setHeaderStyle(columnHeaderStyle);
                }
                final AbstractColumn col = cbldr.build();
                if (!entry.getValue()) {
                    lstColumsSum.add(col);
                }
                drb.addColumn(col);
            }

            cbldr = ColumnBuilder.getNew();
            cbldr.setTitle("TOTAL").setStyle(columnStyle).setHeaderStyle(columnHeaderStyle)
                            .addColumnOperation(ColumnOperation.SUM,
                            (AbstractColumn[]) lstColumsSum.toArray(new AbstractColumn[0]));

            //cbldr.addFieldProperty("net.sf.jasperreports.export.xls.formula", "=SUM(C2+D2)");
            drb.addColumn(cbldr.build());
            //drb.addGlobalColumnVariable(position, column, valueExpression)

            if (print) {
                drb.addAutoText(AutoText.AUTOTEXT_PAGE_X_OF_Y, AutoText.POSITION_FOOTER,
                                AutoText.ALIGMENT_RIGHT, 200, 40);
                drb.addAutoText(AutoText.AUTOTEXT_CREATED_ON, AutoText.POSITION_HEADER, AutoText.ALIGMENT_RIGHT,
                            AutoText.PATTERN_DATE_DATE_TIME);
            }

            drb.setReportLocale(Context.getThreadContext().getLocale());
            final DynamicReport dr = drb.build(); // Finally build the
            // report!
            final JRDataSource ds = getValues4ActionReport(_parameter, columns);
            final JasperPrint jp = DynamicJasperHelper.generateJasperPrint(dr,
                                    print ? new  ClassicLayoutManager() : new ListLayoutManager(), ds);
            ret.put(ReturnValues.VALUES, getFile(jp, mime));
            ret.put(ReturnValues.TRUE, true);
        } catch (final ColumnBuilderException e) {
            throw new EFapsException(Table_Base.class, "ColumnBuilderException", e);
        } catch (final JRException e) {
            throw new EFapsException(Table_Base.class, "JRException", e);
        } catch (final IOException e) {
            throw new EFapsException(Table_Base.class, "IOException", e);
        }

        return ret;
    }

    /**
     * Return the file for the action report.
     *
     * @param _jasperPrint
     * @param _mime document format.
     * @return
     * @throws IOException
     * @throws JRException
     * @throws EFapsException on error.
     */
    private Object getFile(final JasperPrint _jasperPrint,
                           final String _mime) throws IOException, JRException, EFapsException
    {
        File file = null;
        if ("pdf".equalsIgnoreCase(_mime) || _mime == null) {
            file = new FileUtil().getFile("ActionReport", "pdf");
            final FileOutputStream os = new FileOutputStream(file);
            JasperExportManager.exportReportToPdfStream(_jasperPrint, os);
            os.close();
        } else if ("xls".equalsIgnoreCase(_mime)) {
            file = new FileUtil().getFile("ActionReport", "xls");
            final FileOutputStream os = new FileOutputStream(file);
            final JRXlsExporter exporter = new JRXlsExporter();
            _jasperPrint.setName(_jasperPrint.getName().replace("/", "-"));
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, _jasperPrint);
            exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, os);
            exporter.setParameter(JRXlsAbstractExporterParameter.IS_DETECT_CELL_TYPE, Boolean.TRUE);
            exporter.setParameter(JRXlsAbstractExporterParameter.IS_IGNORE_CELL_BORDER, Boolean.TRUE);
            exporter.setParameter(JRXlsAbstractExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_COLUMNS, Boolean.TRUE);
            exporter.setParameter(JRXlsAbstractExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
            exporter.setParameter(JRXlsAbstractExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
            exporter.setParameter(JRXlsAbstractExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
            exporter.setParameter(JRExporterParameter.IGNORE_PAGE_MARGINS, Boolean.TRUE);
            exporter.exportReport();
            os.close();
        }
        return file;
    }

    /**
     * Method to get the action definition of accounting.
     *
     * @return Map with the OID and Name as key and a Boolean if it's related with a neutral payslip case.
     * @throws EFapsException on error.
     */
    private Map<String, Boolean> getActionColums()
        throws EFapsException
    {
        final HashMap<String, Boolean> map = new HashMap<String, Boolean>();
        // Accounting_ActionDefinitionPayrollCasePosition
        final QueryBuilder queryBldr = new QueryBuilder(Type
                        .get(UUID.fromString("9f890acc-0b0f-4d78-ae0a-8070618f130b")));
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute("Name");
        final SelectBuilder selCasePos = new SelectBuilder()
                .linkfrom(CIPayroll.CasePositionAbstract, CIPayroll.CasePositionAbstract.ActionDefinitionAbstractLink)
                .attribute(CIPayroll.CasePositionNeutral.OID);
        multi.addSelect(selCasePos);
        multi.execute();
        while (multi.next()) {
            final Object casePos = multi.getSelect(selCasePos);
            boolean neutral = false;
            if (casePos instanceof String) {
                if (Instance.get((String) casePos).getType().isKindOf(CIPayroll.CasePositionNeutral.getType())) {
                    neutral = true;
                }
            } else if (casePos instanceof List) {
                @SuppressWarnings("unchecked")
                final
                String oidAct = ((List<String>) casePos).get(0);
                if (Instance.get(oidAct).getType().isKindOf(CIPayroll.CasePositionNeutral.getType())) {
                    neutral = true;
                }
            }
            final String name = multi.<String>getAttribute("Name");
            map.put(multi.getCurrentInstance().getOid() + ":" + name, neutral);
        }

        return map;
    }

    /**
     * Get the Style for the different Sections.
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _detail       Section
     * @return Style for Dynamic JAsper
     * @throws EFapsException on error
     */
    protected Style getStyle(final Parameter _parameter,
                             final Section _detail)
        throws EFapsException
    {
        Style ret;
        switch (_detail) {
            case TITLE:
                ret = new Style();
                ret.setFont(Font.VERDANA_BIG_BOLD);
                ret.getFont().setPdfFontEmbedded(true);
                break;
            case COLUMN:
                ret = new Style();
                ret.setFont(Font.VERDANA_MEDIUM);
                ret.setBorder(Border.PEN_1_POINT);
                ret.getFont().setPdfFontEmbedded(true);
                break;
            case COLUMNHEADER:
                ret = new Style();
                ret.setFont(Font.VERDANA_MEDIUM_BOLD);
                ret.setBackgroundColor(Color.gray);
                ret.setBorder(Border.PEN_1_POINT);
                ret.setTransparency(Transparency.OPAQUE);
                ret.setTextColor(Color.white);
                ret.setStreching(Stretching.NO_STRETCH);
                ret.getFont().setPdfFontEmbedded(true);
                break;
            default:
                ret = new Style();
                ret.setFont(Font.VERDANA_MEDIUM);
                ret.getFont().setPdfFontEmbedded(true);
                break;
        }
        return ret;
    }

    /**
     * Method to get the values to put in the report.
     *
     * @param _parameter as passed from eFaps API.
     * @param _columns Map with the columns
     * @return JRMapCollectionDataSource with the data for the report.
     * @throws EFapsException on error.
     */
    public JRMapCollectionDataSource getValues4ActionReport(final Parameter _parameter,
                                                            final Map<String, Boolean> _columns)
        throws EFapsException
    {
        final Map<String, Map<String, ActionPos>> map = new HashMap<String, Map<String, ActionPos>>();
        final DateTime dateTo = new DateTime(_parameter.getParameterValue("dateTo"));
        final DateTime dateFrom = new DateTime(_parameter.getParameterValue("dateFrom"));
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Payslip);
        queryBldr.addWhereAttrLessValue(CIPayroll.Payslip.DueDate, dateTo.plusDays(1));
        queryBldr.addWhereAttrGreaterValue(CIPayroll.Payslip.Date, dateFrom.minusSeconds(1));
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selLEmp = new SelectBuilder().linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                        .attribute(CIHumanResource.Employee.LastName);
        final SelectBuilder selFEmp = new SelectBuilder().linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                        .attribute(CIHumanResource.Employee.FirstName);
        multi.addSelect(selFEmp, selLEmp);
        multi.execute();
        while (multi.next()) {
            final String empName = multi.<String>getSelect(selLEmp) + ", " + multi.<String>getSelect(selFEmp);
            final QueryBuilder queryBldr2 = new QueryBuilder(CIPayroll.PositionAbstract);
            queryBldr2.addWhereAttrNotEqValue(CIPayroll.PositionAbstract.Type, CIPayroll.PositionSum.getType().getId());
            queryBldr2.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink,
                            multi.getCurrentInstance().getId());
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            multi2.addAttribute(CIPayroll.PositionAbstract.Amount);
            final SelectBuilder selAction = new SelectBuilder()
                            .linkto(CIPayroll.PositionAbstract.CasePositionAbstractLink)
                            .linkto(CIPayroll.CasePositionAbstract.ActionDefinitionAbstractLink).oid();
            final SelectBuilder selActionName = new SelectBuilder()
                            .linkto(CIPayroll.PositionAbstract.CasePositionAbstractLink)
                            .linkto(CIPayroll.CasePositionAbstract.ActionDefinitionAbstractLink).attribute("Name");
            multi2.addSelect(selAction, selActionName);
            multi2.execute();
            while (multi2.next()) {
                BigDecimal amount = multi2.<BigDecimal>getAttribute(CIPayroll.PositionAbstract.Amount);
                if (multi2.getCurrentInstance().getType().isKindOf(CIPayroll.PositionDeduction.getType())) {
                    amount = multi2.<BigDecimal>getAttribute(CIPayroll.PositionAbstract.Amount).negate();
                }
                final Instance actInst = Instance.get(multi2.<String>getSelect(selAction));
                final String actName = multi2.<String>getSelect(selActionName);
                if (actInst.isValid()) {
                    if (!map.containsKey(empName)) {
                        final Map<String, ActionPos> map2 = new HashMap<String, ActionPos>();
                        final ActionPos actionPos = new ActionPos(actInst, actName, amount);
                        map2.put(actInst.getOid(), actionPos);
                        map.put(empName, map2);
                    } else {
                        final Map<String, ActionPos> map2 = map.get(empName);
                        if (map2.containsKey(actInst.getOid())) {
                            final ActionPos actionPos = map2.get(actInst.getOid());
                            actionPos.setSumAmount(amount);
                        } else {
                            final ActionPos actionPos = new ActionPos(actInst, actName, amount);
                            map2.put(actInst.getOid(), actionPos);
                            map.put(empName, map2);
                        }
                    }
                }
            }
        }

        final List<Map<String, ?>> lstMap = new ArrayList<Map<String, ?>>();
        for (final Entry<String, Map<String, ActionPos>> entry : map.entrySet()) {
            final Map<String, Object> values = new HashMap<String, Object>();
            values.put("employee", entry.getKey());
            for (final Entry<String, ActionPos> entry2 : entry.getValue().entrySet()) {
                values.put(entry2.getKey(), entry2.getValue().getSumAmount());
            }
            for (final Entry<String, Boolean> entry3 : _columns.entrySet()) {
                final String[] nameOid = entry3.getKey().split(":");
                if (!values.containsKey(nameOid[0])) {
                    values.put(nameOid[0], BigDecimal.ZERO);
                }
            }
            //values.put("total", "=SUM(C2+D2)");
            lstMap.add(values);
        }

        final JRMapCollectionDataSource ds = new JRMapCollectionDataSource(lstMap);
        return ds;
    }

    public class ActionPos
    {
        private final Instance actionInst;
        private final String actionName;
        private BigDecimal sumAmount;

        /**
         * @param _actionInst
         * @param _actionName
         * @param _sumAmount
         */
        public ActionPos(final Instance _actionInst,
                         final String _actionName,
                         final BigDecimal _sumAmount) {
            this.actionInst = _actionInst;
            this.actionName = _actionName;
            this.sumAmount = _sumAmount;
        }

        /**
         * @return the sumAmount
         */
        private BigDecimal getSumAmount()
        {
            return sumAmount;
        }

        /**
         * @param sumAmount the sumAmount to set
         */
        private void setSumAmount(final BigDecimal _sumAmount)
        {
            this.sumAmount = this.sumAmount.add(_sumAmount);
        }

        /**
         * @return the actionInst
         */
        private Instance getActionInst()
        {
            return actionInst;
        }

        /**
         * @return the actionName
         */
        private String getActionName()
        {
            return actionName;
        }


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
        public InsertPos(final SumPosition _position,
                         final Parameter _parameter,
                         final Map<Instance, SumPosition> _sums,
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

        /**
         * @return
         */
        private String getDescription()
        {
            return this.description;
        }

        /**
         * @return
         */
        private Long getId()
        {
            return this.id;
        }

    }
    public class TablePos
    {

        private final List<BigDecimal> values = new ArrayList<BigDecimal>();
        private final List<Integer> pos = new ArrayList<Integer>();

        private boolean setValue = false;
        private final String postfix;
        public TablePos(final BigDecimal _amount,
                        final Integer _pos,
                        final String _postfix)

        {
            getValues().add(_amount);
            this.pos.add(_pos);
            this.postfix = _postfix;
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
    }
    public abstract class Position
    {


        private final Instance instance;

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

        public abstract BigDecimal getResult(final Parameter _parameter,
                                             final Map<Instance, SumPosition> sums,
                                             final Map<Instance, TablePos> _values);

    }

    public class CalcPosition extends Position
    {

        private Instance relInst;
        private Integer denominator;
        private Integer numerator;
        private Instance toInst;
        private final String esjp;
        private final String method;
        /**
         * @param currentInstance
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

        /* (non-Javadoc)
         * @see org.efaps.esjp.payroll.Payslip_Base.Position#getResult(java.util.Map)
         */
        @Override
        public BigDecimal getResult(final Parameter _parameter,
                                    final Map<Instance, SumPosition> _sums,
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
                    final Class<?> cls = Class.forName(this.esjp, true, Payslip_Base.CLASSLOADER);
                    final Method meth = cls.getMethod(this.method, new Class[]{Parameter.class, Map.class, Map.class,
                                    Position.class });
                    ret = (BigDecimal) meth.invoke(cls.newInstance(), _parameter, _sums, _values, this);
                } catch (final SecurityException e) {
                 // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (final ClassNotFoundException e) {
                 // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (final NoSuchMethodException e) {
                 // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (final IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (final IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (final InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (final InstantiationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (this.toInst != null && this.toInst.isValid()) {
                if (_sums.containsKey(this.toInst)) {
                    final SumPosition sum = _sums.get(this.toInst);
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
    }


    public class SumPosition extends Position
    {

        private final String name;

        private final String description;

        private final Set<Position> children = new HashSet<Position>();

        private final Integer sorted;

        private final Integer mode;


        public SumPosition(final Map<Instance, SumPosition> _sums,
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
                    this.children.add(new CalcPosition(multi.getCurrentInstance()));
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
                                    final Map<Instance, SumPosition> _sums,
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
    }
}
