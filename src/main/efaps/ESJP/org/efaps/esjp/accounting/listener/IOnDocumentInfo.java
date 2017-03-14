/*
 * Copyright 2003 - 2017 The eFaps Team
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

package org.efaps.esjp.accounting.listener;

import java.math.BigDecimal;
import java.util.Map;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsNoUpdate;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * This class is only used in case that the Accounting App is not installed to
 * be able to compile the classes.
 *
 * @author The eFaps Team
 * @version $Id: Project.java 5526 2010-09-10 14:17:54Z miguel.a.aranya $
 */
@EFapsUUID("0261a998-4e71-4c73-8f4d-e83bdc70fcd2")
@EFapsApplication("eFapsApp-Payroll")
@EFapsNoUpdate
public interface IOnDocumentInfo
    extends IEsjpListener
{

    /**
     * Gets the key two amount.
     *
     * @param _docInst the doc inst
     * @return the key two amount
     * @throws EFapsException on error
     */
    Map<String, Map<String, BigDecimal>> getKey2Amount(Instance _docInst)
        throws EFapsException;

}
