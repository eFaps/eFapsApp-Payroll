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
 * Revision:        $Rev: 13342 $
 * Last Changed:    $Date: 2014-07-16 12:27:59 -0500 (Wed, 16 Jul 2014) $
 * Last Changed By: $Author: jan@moxter.net $
 */

package org.efaps.esjp.payroll.listener;

import java.math.BigDecimal;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * @author The eFaps Team
 * @version $Id: IOnQuery.java 13342 2014-07-16 17:27:59Z jan@moxter.net $
 */
@EFapsUUID("ddb6a4d9-9226-4131-825c-eddce5120163")
@EFapsApplication("eFapsApp-Payroll")
public interface IOnAdvance
    extends IEsjpListener
{

    BigDecimal getLaborTime(final Parameter _parameter,
                            final Instance _advanceInst,
                            final DateTime _date,
                            final Instance _emplInst)
        throws EFapsException;

    BigDecimal getExtraLaborTime(final Parameter _parameter,
                                 final Instance _advanceInst,
                                 final DateTime _date,
                                 final Instance _emplInst)
        throws EFapsException;

    BigDecimal getNightLaborTime(final Parameter _parameter,
                                 final Instance _advanceInst,
                                 final DateTime _date,
                                 final Instance _emplInst)
        throws EFapsException;

    BigDecimal getHolidayLaborTime(final Parameter _parameter,
                                   final Instance _advanceInst,
                                   final DateTime _date,
                                   final Instance _emplInst)
        throws EFapsException;

}
