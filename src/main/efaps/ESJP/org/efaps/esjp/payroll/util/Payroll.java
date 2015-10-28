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

package org.efaps.esjp.payroll.util;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.IBitEnum;
import org.efaps.admin.datamodel.IEnum;
import org.efaps.admin.datamodel.attributetype.BitEnumType;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.annotation.EFapsSysConfAttribute;
import org.efaps.api.annotation.EFapsSystemConfiguration;
import org.efaps.esjp.admin.common.systemconfiguration.BooleanSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.ListSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.PropertiesSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.StringSysConfAttribute;
import org.efaps.util.cache.CacheReloadException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("e773a372-0d29-41bf-b064-c2fd1f84b279")
@EFapsApplication("eFapsApp-Payroll")
@EFapsSystemConfiguration("6f21b777-3c7d-4792-b3c0-8bfb6af0bf5e")
public final class Payroll
{
    /** The base. */
    public static final String BASE = "org.efaps.payroll.";
    /** Payroll-Configuration. */
    public static final UUID SYSCONFUUID = UUID.fromString("6f21b777-3c7d-4792-b3c0-8bfb6af0bf5e");

    /** See description. */
    @EFapsSysConfAttribute
    public static final ListSysConfAttribute RULESANDBOXWHITELIST = new ListSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "WhiteList4RuleSandbox")
                    .description("List of class names that can be executed in the rule contexts.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute STATICMETHODMAPPING = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "StaticMethodMapping")
                    .description("List of static methods added to the parameters.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RULE4AFPTOTAL = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "RuleKey4AFPTotal")
                    .description("Key of the Rule that contains the total for the AFP Report.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYSLIPEVALLABORTIME = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Payslip.EvaluateLaborTime")
                    .description("Evaluate the LaborTime for Payslip from TimeReports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYSLIPEVALEXTRALABORTIME = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Payslip.EvaluateExtraLaborTime")
                    .description("Evaluate the ExtraLaborTime for Payslip from TimeReports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYSLIPEVALNIGHTLABORTIME = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Payslip.EvaluateNightLaborTime")
                    .description("Evaluate the NightLaborTime for Payslip from TimeReports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYSLIPEVALHOLIDAYLABORTIME = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Payslip.EvaluateHolidayLaborTime")
                    .description("Evaluate the NightLaborTime for Payslip from TimeReports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYSLIPJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Payslip.JasperReport")
                    .description("JasperReport for Payslip.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYSLIPMIME = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Payslip.Mime")
                    .description("Mime for the Payslip report.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ADVANCEEVALLABORTIME = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Advance.EvaluateLaborTime")
                    .description("Evaluate the LaborTime for Payslip from TimeReports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ADVANCEEVALEXTRALABORTIME = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Advance.EvaluateExtraLaborTime")
                    .description("Evaluate the ExtraLaborTime for Payslip from TimeReports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ADVANCEEVALNIGHTLABORTIME = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Advance.EvaluateNightLaborTime")
                    .description("Evaluate the NightLaborTime for Payslip from TimeReports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ADVANCEEVALHOLIDAYLABORTIME = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Advance.EvaluateHolidayLaborTime")
                    .description("Evaluate the NightLaborTime for Payslip from TimeReports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute ADVANCEJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Advance.JasperReport")
                    .description("JasperReport for Advance.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute ADVANCEMIME = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Advance.Mime")
                    .description("Mime for the Advance Report.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PROCESSACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Process.Activate")
                    .description("Evaluate the NightLaborTime for Payslip from TimeReports.");

    /**
     * Singelton.
     */
    private Payroll()
    {
    }

    /**
     * Tipo de Regla.
     */
    public enum RuleType
        implements IEnum
    {
        /**
         * Temporal Summary.
         */
        SUM,
        /**
         * Payment.
         */
        PAYMENT,
        /**
         * Deduction.
         */
        DEDUCTION,
        /**
         * Neutral.
         */
        NEUTRAL;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }

    /**
     * Configuration for Rules.
     */
    public enum RuleConfig
        implements IBitEnum
    {
        /**
         * Exclude it if it is Zero.
         */
        EXCLUDEZERO,
        /**
         * INclude it in the report for PLAME.
         */
        INCLUDEPLAME,
        /**
         * Evaluate for Alteration.;
         */
        EVALUATEALTERATION;

        /**
         * {@inheritDoc}
         */
        @Override
        public int getInt()
        {
            return BitEnumType.getInt4Index(ordinal());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getBitIndex()
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
        return SystemConfiguration.get(SYSCONFUUID);
    }
}
