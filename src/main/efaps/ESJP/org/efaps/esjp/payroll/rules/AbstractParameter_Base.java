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
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsClassLoader;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.payroll.util.Payroll;
import org.efaps.esjp.payroll.util.PayrollSettings;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: AbstractParameter_Base.java 14638 2014-12-17 00:23:45Z
 *          jan@moxter.net $
 * @param <T> type of parameter
 */
@EFapsUUID("45fcf09d-d676-4ac1-8f15-5f790f2eaf03")
@EFapsRevision("$Rev$")
public abstract class AbstractParameter_Base<T>
{

    /**
     * Key for a parameter containing the instance of the employee.
     */
    protected static final String PARAKEY4EMPLOYINST = "EmployeeInstance";

    /**
     * Key for a parameter containing the instance of the document.
     */
    protected static final String PARAKEY4DOCINST = "DocInstance";

    /**
     * Key for a parameter containing the date.
     */
    protected static final String PARAKEY4DATE = "Date";

    /**
     * Key for a parameter containing the Labor Time.
     */
    protected static final String PARAKEY4LT = "LaborTime";

    /**
     * Key for a parameter containing the Extra Labor Time.
     */
    protected static final String PARAKEY4ELT = "ExtraLaborTime";

    /**
     * Key for a parameter containing the Night Labor Time.
     */
    protected static final String PARAKEY4NLT = "NightLaborTime";

    /**
     * Key for a parameter containing the Holiday Labor Time.
     */
    protected static final String PARAKEY4HLT = "HolidayLaborTime";

    /**
     * Key for a parameter containing StartDate.
     */
    protected static final String PARAKEY4STARTDATE = "StartDate";

    /**
     * Key for a parameter containing the Enddate.
     */
    protected static final String PARAKEY4ENDDATE = "EndDate";

    /**
     * Logger for this classes.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractParameter.class);

    /**
     * Instance of the rule.
     */
    private Instance instance;

    /**
     * Key.
     */
    private String key;

    /**
     * Value.
     */
    private Object value;

    /**
     * Description.
     */
    private String description;

    /**
     * @return this used for chaining
     */
    protected abstract T getThis();

    /**
     * Getter method for the instance variable {@link #key}.
     *
     * @return value of instance variable {@link #key}
     */
    public String getKey()
    {
        return this.key;
    }

    /**
     * Setter method for instance variable {@link #key}.
     *
     * @param _key value for instance variable {@link #key}
     * @return this for chaining
     */
    public T setKey(final String _key)
    {
        this.key = _key;
        return getThis();
    }

    /**
     * Getter method for the instance variable {@link #value}.
     *
     * @return value of instance variable {@link #value}
     * @throws EFapsException on error
     */
    public Object getValue()
        throws EFapsException
    {
        return this.value;
    }

    /**
     * Setter method for instance variable {@link #value}.
     *
     * @param _value value for instance variable {@link #value}
     * @return this for chaining
     */
    public T setValue(final Object _value)
    {
        this.value = _value;
        return getThis();
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
     * Setter method for instance variable {@link #description}.
     *
     * @param _description value for instance variable {@link #description}
     * @return this for chaining
     */
    public T setDescription(final String _description)
    {
        this.description = _description;
        return getThis();
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
     * Setter method for instance variable {@link #instance}.
     *
     * @param _instance value for instance variable {@link #instance}
     * @return this for chaining
     */
    public T setInstance(final Instance _instance)
    {
        this.instance = _instance;
        return getThis();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Map of parameters
     * @throws EFapsException on error
     */
    protected static Map<String, Object> getParameters(final Parameter _parameter)
        throws EFapsException
    {
        return getParameters(_parameter, null);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst instance of the document
     * @return Map of parameters
     * @throws EFapsException on error
     */
    protected static Map<String, Object> getParameters(final Parameter _parameter,
                                                       final Instance _docInst)
        throws EFapsException
    {
        Instance docInst = _docInst;
        Instance employeeInst = null;
        if (docInst == null) {
            docInst = _parameter.getInstance();
            if (docInst == null && _parameter.getCallInstance() != null) {
                docInst = _parameter.getCallInstance();
            }
        }
        if (docInst != null) {
            if (docInst.getType().isKindOf(CIPayroll.PositionAbstract)) {
                final PrintQuery print = new PrintQuery(docInst);
                final SelectBuilder sel = SelectBuilder.get()
                                .linkto(CIPayroll.PositionAbstract.DocumentAbstractLink)
                                .linkto(CIPayroll.Payslip.EmployeeAbstractLink).instance();
                print.addSelect(sel);
                print.execute();
                employeeInst = print.getSelect(sel);
            } else if (docInst.getType().isKindOf(CIPayroll.DocumentAbstract)) {
                final PrintQuery print = new PrintQuery(docInst);
                final SelectBuilder sel = SelectBuilder.get()
                                .linkto(CIPayroll.Payslip.EmployeeAbstractLink).instance();
                print.addSelect(sel);
                print.execute();
                employeeInst = print.getSelect(sel);
            }
        } else {
            employeeInst = Instance.get(_parameter.getParameterValue("employee"));
        }
        return getParameters(_parameter, docInst, employeeInst);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst instance of the document
     * @param _employeeInst instance of the employee
     * @return Map of parameters
     * @throws EFapsException on error
     */
    protected static Map<String, Object> getParameters(final Parameter _parameter,
                                                       final Instance _docInst,
                                                       final Instance _employeeInst)
        throws EFapsException
    {
        final Map<String, Object> ret = new HashMap<>();

        final Properties statics = Payroll.getSysConfig().getAttributeValueAsProperties(
                        PayrollSettings.STATICMETHODMAPPING, true);
        for (final Entry<Object, Object> entry : statics.entrySet()) {
            try {
                final Class<?> clazz = Class.forName((String) entry.getValue(), true, EFapsClassLoader.getInstance());
                ret.put((String) entry.getKey(), clazz);
            } catch (final ClassNotFoundException e) {
                LOG.error("Catched ClassNotFoundException", e);
            }
        }

        AbstractParameter.add2Parameters(_parameter, ret, _docInst);
        DateTime date;
        if (ret.containsKey(AbstractParameter.PARAKEY4DATE)) {
            date = (DateTime) ret.get(AbstractParameter.PARAKEY4DATE);
        } else {
            date = new DateTime();
        }
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.ParameterAbstract);
        queryBldr.addWhereAttrGreaterValue(CIPayroll.ParameterAbstract.ValidUntil, date.minusMinutes(1));
        queryBldr.addWhereAttrLessValue(CIPayroll.ParameterAbstract.ValidFrom, date.plusMinutes(1));

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.ParameterAbstract.Key, CIPayroll.ParameterAbstract.Value);
        multi.execute();
        while (multi.next()) {
            if (multi.getCurrentInstance().getType().isCIType(CIPayroll.ParameterFix)) {
                final FixParameter para = new FixParameter()
                                .setInstance(multi.getCurrentInstance())
                                .setKey(multi.<String>getAttribute(CIPayroll.ParameterAbstract.Key))
                                .setValue(multi.<String>getAttribute(CIPayroll.ParameterAbstract.Value));
                ret.put(para.getKey(), para.getValue());
            } else if (multi.getCurrentInstance().getType().isCIType(CIPayroll.ParameterEmployee)
                            && _employeeInst.isValid()) {
                final EmployeeParameter para = new EmployeeParameter()
                                .setInstance(multi.getCurrentInstance())
                                .setKey(multi.<String>getAttribute(CIPayroll.ParameterAbstract.Key))
                                .setSelect(multi.<String>getAttribute(CIPayroll.ParameterAbstract.Value))
                                .setEmployeeInstance(_employeeInst);
                ret.put(para.getKey(), para.getValue());
            }
        }
        if (_employeeInst != null && _employeeInst.isValid()) {
            ret.put(AbstractParameter.PARAKEY4EMPLOYINST, _employeeInst);
        }
        return ret;
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @param _map map to add to
     * @throws EFapsException on error
     */
    protected static void add2Parameters(final Parameter _parameter,
                                         final Map<String, Object> _map)
        throws EFapsException
    {
        AbstractParameter.add2Parameters(_parameter, _map, null);
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @param _map map to add to
     * @param _docInst instance of the document to be evaluated for parameters
     * @throws EFapsException on error
     */
    protected static void add2Parameters(final Parameter _parameter,
                                         final Map<String, Object> _map,
                                         final Instance _docInst)
        throws EFapsException
    {
        if (!_map.containsKey(AbstractParameter.PARAKEY4DOCINST)) {
            if (_docInst != null && _docInst.isValid() && _docInst.getType().isKindOf(CIPayroll.DocumentAbstract)) {
                _map.put(AbstractParameter.PARAKEY4DOCINST, _docInst);
            }
        }

        if (!_map.containsKey(AbstractParameter.PARAKEY4DATE)) {
            DateTime date = null;
            if (_docInst != null && _docInst.isValid() && _docInst.getType().isKindOf(CIPayroll.DocumentAbstract)) {
                final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
                print.addAttribute(CIPayroll.DocumentAbstract.Date, CIPayroll.DocumentAbstract.LaborTime,
                                CIPayroll.DocumentAbstract.ExtraLaborTime, CIPayroll.DocumentAbstract.HolidayLaborTime,
                                CIPayroll.DocumentAbstract.NightLaborTime);
                print.executeWithoutAccessCheck();
                date = print.getAttribute(CIPayroll.DocumentAbstract.Date);
            } else {
                final String dateStr = _parameter.getParameterValue("date_eFapsDate");
                if (dateStr != null) {
                    date = DateUtil.getDateFromParameter(dateStr);
                }
            }
            if (date != null) {
                _map.put(AbstractParameter.PARAKEY4DATE, date);
            }
        }
        if (!_map.containsKey(AbstractParameter.PARAKEY4LT)) {
            BigDecimal laborTime = null;
            if (_docInst != null
                            && _docInst.isValid()
                            && (_docInst.getType().isKindOf(CIPayroll.Payslip) || _docInst.getType().isKindOf(
                                            CIPayroll.Advance))) {
                final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
                print.addAttribute(CIPayroll.DocumentAbstract.Date, CIPayroll.DocumentAbstract.LaborTime,
                                CIPayroll.DocumentAbstract.ExtraLaborTime, CIPayroll.DocumentAbstract.HolidayLaborTime,
                                CIPayroll.DocumentAbstract.NightLaborTime);
                print.executeWithoutAccessCheck();
                final Object[] obj = print.<Object[]>getAttribute(CIPayroll.DocumentAbstract.LaborTime);
                laborTime = obj == null ? null : (BigDecimal) obj[0];
            } else {
                try {
                    final String laborTimeStr = _parameter
                                    .getParameterValue(CIFormPayroll.Payroll_PayslipForm.laborTime.name);
                    if (laborTimeStr != null && !laborTimeStr.isEmpty()) {
                        laborTime = (BigDecimal) NumberFormatter.get().getFormatter().parse(laborTimeStr);
                    } else {
                        laborTime = BigDecimal.ZERO;
                    }
                } catch (final ParseException e) {
                    LOG.warn("Parsing problems", e);
                }
            }
            if (laborTime != null) {
                _map.put(AbstractParameter.PARAKEY4LT, laborTime);
            }
        }
        if (!_map.containsKey(AbstractParameter.PARAKEY4ELT)) {
            BigDecimal laborTime = null;
            if (_docInst != null
                            && _docInst.isValid()
                            && (_docInst.getType().isKindOf(CIPayroll.Payslip) || _docInst.getType().isKindOf(
                                            CIPayroll.Advance))) {
                final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
                print.addAttribute(CIPayroll.DocumentAbstract.Date, CIPayroll.DocumentAbstract.LaborTime,
                                CIPayroll.DocumentAbstract.ExtraLaborTime, CIPayroll.DocumentAbstract.HolidayLaborTime,
                                CIPayroll.DocumentAbstract.NightLaborTime);
                print.executeWithoutAccessCheck();
                final Object[] obj = print.<Object[]>getAttribute(CIPayroll.DocumentAbstract.ExtraLaborTime);
                laborTime = obj == null ? null : (BigDecimal) obj[0];
            } else {
                try {
                    final String laborTimeStr = _parameter
                                    .getParameterValue(CIFormPayroll.Payroll_PayslipForm.extraLaborTime.name);
                    if (laborTimeStr != null && !laborTimeStr.isEmpty()) {
                        laborTime = (BigDecimal) NumberFormatter.get().getFormatter().parse(laborTimeStr);
                    } else {
                        laborTime = BigDecimal.ZERO;
                    }
                } catch (final ParseException e) {
                    LOG.warn("Parsing problems", e);
                }
            }
            if (laborTime != null) {
                _map.put(AbstractParameter.PARAKEY4ELT, laborTime);
            }
        }
        if (!_map.containsKey(AbstractParameter.PARAKEY4HLT)) {
            BigDecimal laborTime = null;
            if (_docInst != null
                            && _docInst.isValid()
                            && (_docInst.getType().isKindOf(CIPayroll.Payslip) || _docInst.getType().isKindOf(
                                            CIPayroll.Advance))) {
                final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
                print.addAttribute(CIPayroll.DocumentAbstract.Date, CIPayroll.DocumentAbstract.LaborTime,
                                CIPayroll.DocumentAbstract.ExtraLaborTime, CIPayroll.DocumentAbstract.HolidayLaborTime,
                                CIPayroll.DocumentAbstract.NightLaborTime);
                print.executeWithoutAccessCheck();
                final Object[] obj = print.<Object[]>getAttribute(CIPayroll.DocumentAbstract.HolidayLaborTime);
                laborTime = obj == null ? null : (BigDecimal) obj[0];
            } else {
                try {
                    final String laborTimeStr = _parameter
                                    .getParameterValue(CIFormPayroll.Payroll_PayslipForm.holidayLaborTime.name);
                    if (laborTimeStr != null && !laborTimeStr.isEmpty()) {
                        laborTime = (BigDecimal) NumberFormatter.get().getFormatter().parse(laborTimeStr);
                    } else {
                        laborTime = BigDecimal.ZERO;
                    }
                } catch (final ParseException e) {
                    LOG.warn("Parsing problems", e);
                }
            }
            if (laborTime != null) {
                _map.put(AbstractParameter.PARAKEY4HLT, laborTime);
            }
        }
        if (!_map.containsKey(AbstractParameter.PARAKEY4NLT)) {
            BigDecimal laborTime = null;
            if (_docInst != null
                            && _docInst.isValid()
                            && (_docInst.getType().isKindOf(CIPayroll.Payslip) || _docInst.getType().isKindOf(
                                            CIPayroll.Advance))) {
                final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
                print.addAttribute(CIPayroll.DocumentAbstract.Date, CIPayroll.DocumentAbstract.LaborTime,
                                CIPayroll.DocumentAbstract.ExtraLaborTime, CIPayroll.DocumentAbstract.HolidayLaborTime,
                                CIPayroll.DocumentAbstract.NightLaborTime);
                print.executeWithoutAccessCheck();
                final Object[] obj = print.<Object[]>getAttribute(CIPayroll.DocumentAbstract.NightLaborTime);
                laborTime = obj == null ? null : (BigDecimal) obj[0];
            } else {
                try {
                    final String laborTimeStr = _parameter
                                    .getParameterValue(CIFormPayroll.Payroll_PayslipForm.nightLaborTime.name);
                    if (laborTimeStr != null && !laborTimeStr.isEmpty()) {
                        laborTime = (BigDecimal) NumberFormatter.get().getFormatter().parse(laborTimeStr);
                    } else {
                        laborTime = BigDecimal.ZERO;
                    }
                } catch (final ParseException e) {
                    LOG.warn("Parsing problems", e);
                }
            }
            if (laborTime != null) {
                _map.put(AbstractParameter.PARAKEY4NLT, laborTime);
            }
        }

        // check first UserInterface then instance
        if (!_map.containsKey(AbstractParameter.PARAKEY4STARTDATE)) {
            DateTime date = null;
            final String dateStr = _parameter
                            .getParameterValue(CIFormPayroll.Payroll_SettlementForm.startDate.name);
            if (dateStr != null && !dateStr.isEmpty()) {
                date = new DateTime(dateStr);
            } else if (dateStr == null
                            && _parameter.getParameters().containsKey(
                                            CIFormPayroll.Payroll_SettlementForm.startDate.name + "_eFapsDate")) {
                date = DateUtil.getDateFromParameter(_parameter
                                .getParameterValue(CIFormPayroll.Payroll_SettlementForm.startDate.name + "_eFapsDate"));
            } else if (_docInst != null && _docInst.isValid() && _docInst.getType().isKindOf(CIPayroll.Settlement)) {
                final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
                print.addAttribute(CIPayroll.Settlement.StartDate, CIPayroll.Settlement.EndDate);
                print.executeWithoutAccessCheck();
                date = print.getAttribute(CIPayroll.Settlement.StartDate);
            }

            if (date != null) {
                _map.put(AbstractParameter.PARAKEY4STARTDATE, date);
            }
        }

        if (!_map.containsKey(AbstractParameter.PARAKEY4ENDDATE)) {
            DateTime date = null;
            final String dateStr = _parameter
                            .getParameterValue(CIFormPayroll.Payroll_SettlementForm.endDate.name);
            if (dateStr != null && !dateStr.isEmpty()) {
                date = new DateTime(dateStr);
            } else if (dateStr == null
                            && _parameter.getParameters().containsKey(
                                            CIFormPayroll.Payroll_SettlementForm.endDate.name + "_eFapsDate")) {
                date = DateUtil.getDateFromParameter(_parameter
                                .getParameterValue(CIFormPayroll.Payroll_SettlementForm.endDate.name + "_eFapsDate"));
            } else if (_docInst != null && _docInst.isValid() && _docInst.getType().isKindOf(CIPayroll.Settlement)) {
                final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
                print.addAttribute(CIPayroll.Settlement.StartDate, CIPayroll.Settlement.EndDate);
                print.executeWithoutAccessCheck();
                date = print.getAttribute(CIPayroll.Settlement.EndDate);
            }

            if (date != null) {
                _map.put(AbstractParameter.PARAKEY4ENDDATE, date);
            }
        }
    }
}
