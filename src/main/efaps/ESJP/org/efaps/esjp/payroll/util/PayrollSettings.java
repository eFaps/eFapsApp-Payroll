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


package org.efaps.esjp.payroll.util;

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */

@EFapsUUID("00a55935-6b51-4982-ad22-79067ff170c3")
@EFapsRevision("$Rev$")
public interface PayrollSettings
{
    /**
     * String with the name of the selected company.
     */
    String MOVEMENTTYPE_ENDDATE = "org.efaps.payroll.MovementTypeEndDate";

    /**
     * String with the name of the selected company.
     */
    String RULESANDBOXWHITELIST = "org.efaps.payroll.WhiteList4RuleSandbox";

    /**
     * String with the name of the selected company.
     */
    String STATICMETHODMAPPING = "org.efaps.payroll.StaticMethodMapping";
}
