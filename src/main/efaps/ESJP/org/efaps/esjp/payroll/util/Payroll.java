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

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.IEnum;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.cache.CacheReloadException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("e773a372-0d29-41bf-b064-c2fd1f84b279")
@EFapsRevision("$Rev$")

public final class Payroll
{
    /**
     * Singelton.
     */
    private Payroll()
    {
    }

    public enum RuleType
        implements IEnum
    {
        NONE,
        PAYMENT,
        DEDUCTION,
        NEUTRAL;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }

    /**
     * @return the SystemConfigruation for Payroll
     * @throws CacheReloadException on error
     */
    public static SystemConfiguration getSysConfig()
        throws CacheReloadException
    {
        // Payroll-Configuration
        return SystemConfiguration.get(UUID.fromString("6f21b777-3c7d-4792-b3c0-8bfb6af0bf5e"));
    }
}
