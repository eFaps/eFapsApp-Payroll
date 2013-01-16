/*
 * Copyright 2003 - 2009 The eFaps Team
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
 * Revision:        $Rev: 295 $
 * Last Changed:    $Date: 2011-06-06 15:37:52 -0500 (lun, 06 jun 2011) $
 * Last Changed By: $Author: Jorge Cueva $
 */
package org.efaps.esjp.payroll.reports;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: WorkOrderCalibrateDataSource.java 268 2011-04-29 17:10:40Z
 *          Jorge Cueva $
 */
@EFapsUUID("382d554b-7372-4b6a-a89d-1affcae5e333")
@EFapsRevision("$Rev: 295 $")
public class PayslipReport_Base extends Reports
{

    private static String format = "0601";

    /**
     * Enum used to define the keys for the map.
     */
    public enum Field implements Column
    {
        ID("", 6, null),
        /** */
        DOCTYPE("docType", 2, null),
        /** */
        DOCNUM("docNum", 15, null),
        /** */
        REMUCODE("remuCode", 4, null),
        /** */
        AMOUNT("amount", 10, 2),
        /** */
        PAYAMOUNT("payAmount", 10, 2);
        /**
         * key.
         */
        private final String key;

        /**
         * length.
         */
        private final Integer lenght;

        /**
         * decimalLength.
         */
        private final Integer decimalLength;

        /**
         * @param _key key
         */
        private Field(final String _key,
                      final Integer _length,
                      final Integer _decimalLength)
        {
            this.key = _key;
            this.lenght = _length;
            this.decimalLength = _decimalLength;
        }

        /**
         * Getter method for the instance variable {@link #key}.
         *
         * @return value of instance variable {@link #key}
         */
        @Override
        public String getKey()
        {
            return this.key;
        }

        @Override
        public Integer getLength()
        {
            return this.lenght;
        }

        @Override
        public Integer getDecimalLength()
        {
            return this.decimalLength;
        }

        @Override
        public boolean isOptional()
        {
            return false;
        }

        @Override
        public String getDefaultVal()
        {
            return null;
        }

    }

    public Return generatePayslipReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final DateTime dateFrom = new DateTime(_parameter.getParameterValue("dateFrom"));

        final String name = getName4TextFile(PayslipReport_Base.format, dateFrom);

        File file;
        try {
            file = new FileUtil().getFile(name == null ? "TXT" : name, "txt");
            final PrintWriter writer = new PrintWriter(file);
            writer.print(getReportDataText(_parameter).toString());
            writer.close();
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        } catch (final IOException e) {
            throw new EFapsException(PayslipReport_Base.class, "execute.IOException", e);
        }

        return ret;
    }

    protected String getReportDataText(final Parameter _parameter)
        throws EFapsException
    {
        final DateTime dateFrom = new DateTime(_parameter.getParameterValue("dateFrom"));
        final DateTime dateTo = new DateTime(_parameter.getParameterValue("dateTo"));
        final StringBuilder rep = new StringBuilder();
        final List<Map<String, Object>> values = getReportData(dateFrom, dateTo);

        int cont = 1;
        for (final Map<String, Object> map : values) {
            rep.append(getCharacterValue(map.get(PayslipReport_Base.Field.DOCTYPE.getKey()),
                                PayslipReport_Base.Field.DOCTYPE)).append(getSeparator())
                .append(getCharacterValue(map.get(PayslipReport_Base.Field.DOCNUM.getKey()),
                                PayslipReport_Base.Field.DOCNUM)).append(getSeparator())
                .append(getNumberValue(map.get(PayslipReport_Base.Field.REMUCODE.getKey()),
                                PayslipReport_Base.Field.REMUCODE)).append(getSeparator())
                .append(getNumberValue(map.get(PayslipReport_Base.Field.AMOUNT.getKey()),
                                PayslipReport_Base.Field.AMOUNT)).append(getSeparator())
                .append(getNumberValue(map.get(PayslipReport_Base.Field.PAYAMOUNT.getKey()),
                                PayslipReport_Base.Field.PAYAMOUNT))
                .append("\n");
            cont++;
        }
        return rep.toString();
    }

    protected List<Map<String, Object>> getReportData(final DateTime _dateFrom,
                                                        final DateTime _dateTo)
        throws EFapsException
    {
        final List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip);
        attrQueryBldr.addWhereAttrGreaterValue(CIPayroll.Payslip.Date, _dateFrom);
        attrQueryBldr.addWhereAttrLessValue(CIPayroll.Payslip.DueDate, _dateTo);
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIPayroll.Payslip.ID);

        final QueryBuilder attrQueryBldr2 = new QueryBuilder(CIPayroll.Payslip);
        attrQueryBldr2.addWhereAttrEqValue(CIPayroll.Payslip.Date, _dateFrom);
        attrQueryBldr2.addWhereAttrEqValue(CIPayroll.Payslip.DueDate, _dateTo);
        final AttributeQuery attrQuery2 = attrQueryBldr2.getAttributeQuery(CIPayroll.Payslip.ID);

        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Payslip);
        queryBldr.addWhereAttrInQuery(CIPayroll.Payslip.ID, attrQuery);
        queryBldr.addWhereAttrInQuery(CIPayroll.Payslip.ID, attrQuery2);
        queryBldr.setOr(true);

        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selDoc = new SelectBuilder()
                    .linkto(CIPayroll.Payslip.EmployeeAbstractLink).attribute(CIHumanResource.Employee.Number);
        final SelectBuilder selDocType = new SelectBuilder()
                    .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                    .linkto(CIHumanResource.Employee.NumberTypeLink)
                    .attribute(CIHumanResource.AttributeDefinitionNumberType.Value);
        multi.addSelect(selDoc, selDocType);
        multi.execute();
        while (multi.next()) {
            final String doc = multi.<String>getSelect(selDoc);
            final String docType = multi.<String>getSelect(selDocType);

            final QueryBuilder queryBldr2 = new QueryBuilder(CIPayroll.PositionAbstract);
            queryBldr2.addWhereAttrNotEqValue(CIPayroll.PositionAbstract.Type, CIPayroll.PositionSum.getType().getId());
            queryBldr2.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink,
                            multi.getCurrentInstance().getId());
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            multi2.addAttribute(CIPayroll.PositionAbstract.Amount);
            final SelectBuilder selCaseName = new SelectBuilder()
                    .linkto(CIPayroll.PositionAbstract.CasePositionAbstractLink)
                    .attribute(CIPayroll.CasePositionAbstract.Name);
            final SelectBuilder selCaseExp = new SelectBuilder()
                    .linkto(CIPayroll.PositionAbstract.CasePositionAbstractLink)
                    .attribute(CIPayroll.CasePositionCalc.ExportReport);
            multi2.addSelect(selCaseName, selCaseExp);
            multi2.execute();
            while (multi2.next()) {
                final Map<String, Object> value = new HashMap<String, Object>();
                final BigDecimal amount = multi2.<BigDecimal>getAttribute(CIPayroll.PositionAbstract.Amount);
                final String caseName = multi2.<String>getSelect(selCaseName);
                final Boolean caseNameExp = multi2.<Boolean>getSelect(selCaseExp);

                if (caseNameExp != null && caseNameExp) {
                    value.put(PayslipReport_Base.Field.DOCNUM.getKey(), doc);
                    value.put(PayslipReport_Base.Field.DOCTYPE.getKey(), docType);
                    value.put(PayslipReport_Base.Field.REMUCODE.getKey(), caseName);
                    value.put(PayslipReport_Base.Field.AMOUNT.getKey(), amount);
                    value.put(PayslipReport_Base.Field.PAYAMOUNT.getKey(), amount);
                    values.add(value);
                }
            }
        }

        Collections.sort(values, new Comparator<Map<String, Object>>()
        {
            @Override
            public int compare(final Map<String, Object> _o1,
                               final Map<String, Object> _o2)
            {
                final String name1 = (String) _o1.get(PayslipReport_Base.Field.DOCNUM.getKey());
                final String name2 = (String) _o2.get(PayslipReport_Base.Field.DOCNUM.getKey());
                final int ret = name1.compareTo(name2);
                return ret;
            }
        });
        return values;
    }

}
