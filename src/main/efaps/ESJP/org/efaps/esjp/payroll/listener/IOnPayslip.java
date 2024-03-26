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
package org.efaps.esjp.payroll.listener;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.db.Instance;
import org.efaps.esjp.erp.CommonDocument_Base.CreatedDoc;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * @author The eFaps Team
 * @version $Id: IOnQuery.java 13342 2014-07-16 17:27:59Z jan@moxter.net $
 */
@EFapsUUID("ddb6a4d9-9226-4131-825c-eddce5120163")
@EFapsApplication("eFapsApp-Payroll")
public interface IOnPayslip
    extends IEsjpListener
{

    BigDecimal getLaborTime(final Parameter _parameter,
                            final Instance _payslipInst,
                            final DateTime _date,
                            final DateTime _dueDate,
                            final Instance _emplInst)
        throws EFapsException;

    BigDecimal getExtraLaborTime(final Parameter _parameter,
                                 final Instance _payslipInst,
                                 final DateTime _date,
                                 final DateTime _dueDate,
                                 final Instance _emplInst)
        throws EFapsException;

    BigDecimal getNightLaborTime(final Parameter _parameter,
                                 final Instance _payslipInst,
                                 final DateTime _date,
                                 final DateTime _dueDate,
                                 final Instance _emplInst)
        throws EFapsException;

    BigDecimal getHolidayLaborTime(final Parameter _parameter,
                                   final Instance _payslipInst,
                                   final DateTime _date,
                                   final DateTime _dueDate,
                                   final Instance _emplInst)
        throws EFapsException;

    /**
     * @param _parameter
     * @param _instance
     * @param _map
     * @throws EFapsException on error
     */
    void add2UpdateMap4Employee(final Parameter _parameter,
                                final Instance _employeeInstance,
                                final Map<String, Object> _map)
        throws EFapsException;

    /**
     * @param _parameter
     * @param _createdDoc
     * @return
     */
    void afterCreate(final Parameter _parameter,
                     final CreatedDoc _createdDoc)
        throws EFapsException;

    /**
     * @param _parameter
     * @param _emplInst
     * @return
     */
    Collection<? extends Instance> getEmployeeTimeCardInst(final Parameter _parameter,
                                                           final Instance _emplInst)
        throws EFapsException;

}
