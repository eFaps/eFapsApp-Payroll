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
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.payroll;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.sales.document.AbstractDocument;
import org.efaps.esjp.sales.payment.PaymentDeposit;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
public abstract class BulkPayment_Base
    extends AbstractDocument
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new CreatedDoc
     * @throws EFapsException on error
     */
    public CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc ret = new CreatedDoc();
        final Instance baseInst = Currency.getBaseCurrency();
        final Insert insert = new Insert(CIPayroll.BulkPayment);
        insert.add(CIPayroll.BulkPayment.Date,
                        _parameter.getParameterValue(CIFormPayroll.Payroll_BulkPaymentForm.date.name));
        insert.add(CIPayroll.BulkPayment.Name,
                        _parameter.getParameterValue(CIFormPayroll.Payroll_BulkPaymentForm.name.name));
        insert.add(CIPayroll.BulkPayment.BulkDefinitionLink,
                        _parameter.getParameterValue(CIFormPayroll.Payroll_BulkPaymentForm.bulkDefinition.name));
        insert.add(CIPayroll.BulkPayment.CrossTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.CrossTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.NetTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.DiscountTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.RateCrossTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.RateNetTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.CurrencyId, baseInst);
        insert.add(CIPayroll.BulkPayment.RateCurrencyId, baseInst);
        insert.add(CIPayroll.BulkPayment.Rate, new Object[] { 1, 1 });
        insert.add(CIPayroll.BulkPayment.Status, Status.find(CISales.BulkPaymentStatus.Open));
        insert.execute();
        ret.setInstance(insert.getInstance());

        final Insert relinsert = new Insert(CISales.BulkPayment2Account);
        relinsert.add(CISales.BulkPayment2Account.FromLink, insert.getId());
        relinsert.add(CISales.BulkPayment2Account.ToLink,
                        _parameter.getParameterValue(CIFormPayroll.Payroll_BulkPaymentForm.account4create.name));
        relinsert.execute();

        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new CreatedDoc
     * @throws EFapsException on error
     */
    public Return connectDoc(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> insts = getSelectedInstances(_parameter);

        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        final SelectBuilder selAccInst = SelectBuilder.get().linkfrom(CISales.BulkPayment2Account.FromLink)
                        .linkto(CISales.BulkPayment2Account.ToLink).instance();
        print.addSelect(selAccInst);
        print.addAttribute(CIPayroll.BulkPayment.Name);
        print.execute();
        final Instance accInst = print.getSelect(selAccInst);
        final String name = print.getAttribute(CIPayroll.BulkPayment.Name);

        for (final Instance inst : insts) {
            final PrintQuery docPrint = new PrintQuery(inst);
            docPrint.addAttribute(CIPayroll.DocumentAbstract.CrossTotal);
            docPrint.execute();

            final Parameter parameter = ParameterUtil.clone(_parameter);
            ParameterUtil.setParmeterValue(parameter, "account", String.valueOf(accInst.getId()));
            ParameterUtil.setParmeterValue(parameter, "name", name);
            ParameterUtil.setParmeterValue(parameter, "amount",
                            String.valueOf(docPrint.getAttribute(CIPayroll.DocumentAbstract.CrossTotal)));
            ParameterUtil.setParmeterValue(parameter, "paymentAmount",
                            String.valueOf(docPrint.getAttribute(CIPayroll.DocumentAbstract.CrossTotal)));
            ParameterUtil.setParmeterValue(parameter, getFieldName4Attribute(_parameter,
                            CISales.Payment.CreateDocument.name), inst.getOid());
            ParameterUtil.setParmeterValue(parameter, "paymentRate", "1");
            ParameterUtil.setParmeterValue(parameter, "paymentRate" + RateUI.INVERTEDSUFFIX, "false");

            final PaymentDeposit deposit = new PaymentDeposit()
            {

                @Override
                protected void add2DocCreate(final Parameter _parameter,
                                             final Insert _insert,
                                             final CreatedDoc _createdDoc)
                    throws EFapsException
                {
                    super.add2DocCreate(_parameter, _insert, _createdDoc);
                    final DateTime date = new DateTime();
                    _insert.add(CISales.PaymentDocumentAbstract.Date, date);
                    _createdDoc.getValues().put(CISales.PaymentDocumentAbstract.Date.name, date);
                    final Object[] rateObj = new Object[] { BigDecimal.ONE, BigDecimal.ONE };
                    _insert.add(CISales.PaymentDocumentAbstract.Rate, rateObj);
                    _createdDoc.getValues().put(CISales.PaymentDocumentAbstract.Rate.name, rateObj);
                }

                @Override
                protected void executeAutomation(final Parameter _parameter,
                                                 final CreatedDoc _createdDoc)
                    throws EFapsException
                {
                    final Insert insert = new Insert(CIPayroll.BulkPayment2PaymentDocument);
                    insert.add(CIPayroll.BulkPayment2PaymentDocument.FromLink, _parameter.getInstance());
                    insert.add(CIPayroll.BulkPayment2PaymentDocument.ToLink, _createdDoc.getInstance());
                    insert.execute();
                    super.executeAutomation(_parameter, _createdDoc);
                }
            };
            deposit.create(parameter);
        }
        return new Return();
    }

    public Return getReport4Detail(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(CIPayroll.BulkPayment.getType().getLabel());
        final String html = dyRp.getHtml(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    public Return exportReport4Detail(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(CIPayroll.BulkPayment.getType().getLabel());
        File file = null;
        if ("xls".equalsIgnoreCase(mime)) {
            file = dyRp.getExcel(_parameter);
        } else if ("pdf".equalsIgnoreCase(mime)) {
            file = dyRp.getPDF(_parameter);
        }
        ret.put(ReturnValues.VALUES, file);
        ret.put(ReturnValues.TRUE, true);
        return ret;
    }

    protected AbstractDynamicReport getReport(final Parameter _parameter)
    {
        return new Report4Detail();
    }

    public class Report4Detail
        extends AbstractDynamicReport
    {

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final DRDataSource ret = new DRDataSource("employeeNumber", "employeeLastName", "employeeSecondLastName",
                            "employeeFirstName", "accountNumber", "docName", "amount");
            final List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();

            final Map<Instance, String> employee2Acc = new HashMap<>();
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CIPayroll.BulkPayment.BulkDefinitionLink);
            print.execute();

            final QueryBuilder queryBldrCont = new QueryBuilder(CIPayroll.Employee2BulkPaymentDefinition);
            queryBldrCont.addWhereAttrEqValue(CIPayroll.Employee2BulkPaymentDefinition.ToLink,
                            print.getAttribute(CIPayroll.BulkPayment.BulkDefinitionLink));
            final MultiPrintQuery multiCont = queryBldrCont.getPrint();
            multiCont.addAttribute(CIPayroll.Employee2BulkPaymentDefinition.AccountNumber);
            final SelectBuilder selEmployeeInst = new SelectBuilder()
                            .linkto(CIPayroll.Employee2BulkPaymentDefinition.FromLink).instance();
            multiCont.addSelect(selEmployeeInst);
            multiCont.execute();
            while (multiCont.next()) {
                final Instance employeeInst = multiCont.getSelect(selEmployeeInst);
                final String account = multiCont.getAttribute(CIPayroll.Employee2BulkPaymentDefinition.AccountNumber);
                employee2Acc.put(employeeInst, account);
            }

            // TODO add more status
            final QueryBuilder pdAttrQueryBldr = new QueryBuilder(CISales.PaymentDocumentOutAbstract);
            pdAttrQueryBldr.addWhereAttrNotEqValue(CISales.PaymentDocumentOutAbstract.StatusAbstract,
                            Status.find(CISales.PaymentDepositOutStatus.Canceled));
            final AttributeQuery pdAttrQuery = pdAttrQueryBldr.getAttributeQuery(CISales.PaymentDocumentOutAbstract.ID);

            final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.BulkPayment2PaymentDocument);
            attrQueryBldr.addWhereAttrEqValue(CIPayroll.BulkPayment2PaymentDocument.FromLink, _parameter.getInstance());
            attrQueryBldr.addWhereAttrInQuery(CIPayroll.BulkPayment2PaymentDocument.ToLink, pdAttrQuery);
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CIPayroll.BulkPayment2PaymentDocument.ToLink);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
            queryBldr.addWhereAttrInQuery(CISales.Payment.TargetDocument, attrQuery);
            final MultiPrintQuery multi = queryBldr.getPrint();

            final SelectBuilder selDoc = SelectBuilder.get().linkto(CISales.Payment.CreateDocument);
            final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
            final SelectBuilder selDocName = new SelectBuilder().linkto(CISales.Payment.CreateDocument).attribute(
                            CIERP.DocumentAbstract.Name);

            final SelectBuilder selEmployee = SelectBuilder.get().linkto(
                            CIPayroll.DocumentAbstract.EmployeeAbstractLink);
            final SelectBuilder selEmployeeInst2 = new SelectBuilder(selEmployee).instance();
            final SelectBuilder selEmployeeNumber = new SelectBuilder(selEmployee)
                            .attribute(CIHumanResource.Employee.Number);
            final SelectBuilder selEmployeeFirstName = new SelectBuilder(selEmployee)
                            .attribute(CIHumanResource.Employee.FirstName);
            final SelectBuilder selEmployeeLastName = new SelectBuilder(selEmployee)
                            .attribute(CIHumanResource.Employee.LastName);
            final SelectBuilder selEmployeeSecondLastName = new SelectBuilder(selEmployee)
                            .attribute(CIHumanResource.Employee.SecondLastName);

            multi.addSelect(selDocInst, selDocName);
            multi.addAttribute(CISales.Payment.Amount);
            multi.execute();
            while (multi.next()) {
                final Map<String, Object> map = new HashMap<String, Object>();
                final Instance docInst = multi.getSelect(selDocInst);
                final PrintQuery docPrint = new PrintQuery(docInst);
                docPrint.addSelect(selEmployeeInst2, selEmployeeNumber, selEmployeeFirstName, selEmployeeLastName,
                                selEmployeeSecondLastName);
                docPrint.execute();

                final Instance employeeInst2 = docPrint.getSelect(selEmployeeInst2);
                values.add(map);
                map.put("amount", multi.getAttribute(CISales.Payment.Amount));
                map.put("docName", multi.getSelect(selDocName));
                map.put("employeeNumber", docPrint.getSelect(selEmployeeNumber));
                map.put("employeeFirstName", docPrint.getSelect(selEmployeeFirstName));
                map.put("employeeLastName", docPrint.getSelect(selEmployeeLastName));
                map.put("employeeSecondLastName", docPrint.getSelect(selEmployeeSecondLastName));
                map.put("accountNumber",
                                employee2Acc.containsKey(employeeInst2) ? employee2Acc.get(employeeInst2) : "");

            }

            Collections.sort(values, new Comparator<Map<String, Object>>()
            {

                @Override
                public int compare(final Map<String, Object> _map,
                                   final Map<String, Object> _map1)
                {
                    return _map.get("employeeLastName").toString().compareTo(_map1.get("employeeLastName").toString());
                }
            });

            for (final Map<String, Object> map : values) {
                ret.add(map.get("employeeNumber"),
                                map.get("employeeLastName"),
                                map.get("employeeSecondLastName"),
                                map.get("employeeFirstName"),
                                map.get("accountNumber"),
                                map.get("docName"),
                                map.get("amount"));
            }

            return ret;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<String> employeeNumberColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.payroll.BulkPayment.Report4Detail.employeeNumber"),
                            "employeeNumber", DynamicReports.type.stringType());
            final TextColumnBuilder<String> employeeLastNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.payroll.BulkPayment.Report4Detail.employeeLastName"),
                            "employeeLastName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> employeeSecondLastNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.payroll.BulkPayment.Report4Detail.employeeSecondLastName"),
                            "employeeSecondLastName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> employeeFirstNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.payroll.BulkPayment.Report4Detail.employeeFirstName"),
                            "employeeFirstName", DynamicReports.type.stringType());

            final TextColumnBuilder<String> accountNumberColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.payment.BulkPayment.Report4Detail.accountNumber"),
                            "accountNumber", DynamicReports.type.stringType());
            final TextColumnBuilder<String> docNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.payment.BulkPayment.Report4Detail.docName"),
                            "docName", DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> amountColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.payment.BulkPayment.Report4Detail.amount"),
                            "amount", DynamicReports.type.bigDecimalType());

            final AggregationSubtotalBuilder<BigDecimal> subtotal = DynamicReports.sbt.sum(amountColumn);

            _builder.addColumn(employeeNumberColumn, employeeLastNameColumn, employeeSecondLastNameColumn,
                            employeeFirstNameColumn, accountNumberColumn, docNameColumn, amountColumn);
            _builder.addSubtotalAtColumnFooter(subtotal);
        }
    }

}
