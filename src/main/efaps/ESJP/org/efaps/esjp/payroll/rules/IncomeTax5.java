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
package org.efaps.esjp.payroll.rules;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;


/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_Base</code>"
 * class.
 *
 * @author The eFaps Team
 * 
 */
@EFapsUUID("92b96ec0-a0c7-4303-b529-123784d8651b")
@EFapsApplication("eFapsApp-Payroll")
public class IncomeTax5
    extends IncomeTax5_Base
{
    public final static String KEYS4PAYMENT = IncomeTax5_Base.KEYS4PAYMENT;

    public final static String KEYS4EXTRA = IncomeTax5_Base.KEYS4EXTRA;

    public final static String KEYS4TAX = IncomeTax5_Base.KEYS4TAX;

}
