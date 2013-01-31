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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.payroll.Payslip_Base.Position;
import org.efaps.esjp.payroll.Payslip_Base.TablePos;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Contains method called to calculate one position in a payslip.
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f1cb9622-af14-4f91-a6e3-ae1832165d84")
@EFapsRevision("$Rev$")
public abstract class PayslipCalculator_Base
{
    /**
     * Key to store in the Session.
     */
    public static final String ADVANCE_PAYMENTS = "org.efaps.esjp.payroll.PayslipCalculator.AdvancePay";

    /**
     * Logger for this classes.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(PayslipCalculator_Base.class);

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _sums         mapping of instances to sum positions
     * @param _values       mapping of instances to table positions
     * @param _position     current position
     * @return  sum of advances payed
     * @throws EFapsException on error
     */
    public BigDecimal overtime(final Parameter _parameter,
                               final Map<Instance, Position> _sums,
                               final Map<Instance, TablePos> _values,
                               final Position _position)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final String overtime = _parameter.getParameterValue(CIFormPayroll.Payroll_PayslipForm.extraLaborTime.name);
        if (overtime != null && !overtime.isEmpty()) {
            final DecimalFormat formater = new Payslip().getFormater(0, 2);
            try {
                ret = (BigDecimal) formater.parse(overtime);
            } catch (final ParseException e) {
                PayslipCalculator_Base.LOG.error("error parsing: {} ", overtime, e);
            }
        }
        if (ret.compareTo(BigDecimal.ZERO) > 0) {
            final Position pos = getPosition4Parameter(_parameter, null, _sums, _values, _position);
            final BigDecimal base = pos == null ? BigDecimal.ZERO
                            : pos.getResult(_parameter, _sums, _values);
            if (base.compareTo(BigDecimal.ZERO) > 0) {
                ret = base.setScale(8, BigDecimal.ROUND_HALF_UP)
                                .divide(BigDecimal.valueOf(30), BigDecimal.ROUND_HALF_UP)
                                .divide(BigDecimal.valueOf(8), BigDecimal.ROUND_HALF_UP).multiply(ret);
            }
        }
        if (_values.containsKey(_position.getInstance())) {
            _values.get(_position.getInstance()).setSetValue(true);
            final int count = _values.get(_position.getInstance()).getValues().size();
            _values.get(_position.getInstance()).getValues().clear();
            for (int i = 0; i < count; i++) {
                _values.get(_position.getInstance()).getValues().add(ret);
            }
        }
        return ret;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _sums         mapping of instances to sum positions
     * @param _values       mapping of instances to table positions
     * @param _position     current position
     * @return  sum of advances payed
     * @throws EFapsException on error
     */
    @SuppressWarnings("unchecked")
    public BigDecimal advance(final Parameter _parameter,
                              final Map<Instance, Position> _sums,
                              final Map<Instance, TablePos> _values,
                              final Position _position)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        // get the instance of the employee the payslip belongs to
        final Instance emplInst = Instance.get(_parameter
                        .getParameterValue(CIFormPayroll.Payroll_PayslipForm.number.name));

        if (emplInst.isValid() && emplInst.getType().isKindOf(CIHumanResource.EmployeeAbstract.getType())) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip2Advance);
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIPayroll.Payslip2Advance.ToLink);

            final QueryBuilder queryBlder = new QueryBuilder(CIPayroll.Advance);
            queryBlder.addWhereAttrEqValue(CIPayroll.Advance.EmployeeAbstractLink, emplInst.getId());
            queryBlder.addWhereAttrNotInQuery(CIPayroll.Advance.ID, attrQuery);
            final MultiPrintQuery multi = queryBlder.getPrint();
            multi.addAttribute(CIPayroll.Advance.Amount2Pay);
            multi.execute();
            while (multi.next()) {
                Map<String, Instance> mapAdv = new HashMap<String, Instance>();
                if (Context.getThreadContext().getSessionAttribute(PayslipCalculator_Base.ADVANCE_PAYMENTS) != null) {
                    mapAdv = (Map<String, Instance>) Context.getThreadContext()
                                    .getSessionAttribute(PayslipCalculator_Base.ADVANCE_PAYMENTS);
                } else {
                    Context.getThreadContext().setSessionAttribute(PayslipCalculator_Base.ADVANCE_PAYMENTS, mapAdv);
                }
                mapAdv.put(multi.getCurrentInstance().getOid(), multi.getCurrentInstance());
                final BigDecimal pay = multi.<BigDecimal>getAttribute(CIPayroll.Advance.Amount2Pay);
                if (pay != null) {
                    ret = ret.add(pay);
                }
            }
            if (_values.containsKey(_position.getInstance())) {
                _values.get(_position.getInstance()).setSetValue(true);
                final int count = _values.get(_position.getInstance()).getValues().size();
                _values.get(_position.getInstance()).getValues().clear();
                for (int i = 0; i < count; i++) {
                    _values.get(_position.getInstance()).getValues().add(ret);
                }
            }
        }
        return ret;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _key          key to the position
     * @param _sums         mapping of instances to sum positions
     * @param _values       mapping of instances to table positions
     * @param _position     current position
     * @return Position
     * @throws EFapsException
     */
    protected Position getPosition4Parameter(final Parameter _parameter,
                                             final String _key,
                                             final Map<Instance, Position> _sums,
                                             final Map<Instance, TablePos> _values,
                                             final Position _position)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.CasePositionCalc2Position4Parameter);
        queryBldr.addWhereAttrEqValue(CIPayroll.CasePositionCalc2Position4Parameter.FromAbstractLink,
                        _position.getInstance().getId());

        if (_key != null) {
            queryBldr.addWhereAttrEqValue(CIPayroll.CasePositionCalc2Position4Parameter.Parameter, _key);
        }

        final MultiPrintQuery multi = queryBldr.getPrint();

        final SelectBuilder instSel = new SelectBuilder().linkto(
                        CIPayroll.CasePositionCalc2Position4Parameter.ToAbstractLink).instance();

        multi.addSelect(instSel);
        multi.execute();
        multi.next();
        final Instance relInst = multi.<Instance>getSelect(instSel);
        return _sums.get(relInst);
    }
}
