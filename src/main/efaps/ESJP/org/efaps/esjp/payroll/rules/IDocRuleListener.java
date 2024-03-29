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

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * 
 */
@EFapsUUID("5e072db0-e198-4d11-95e7-17de3bf1aee1")
@EFapsApplication("eFapsApp-Payroll")
public interface IDocRuleListener
    extends IRuleListener
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst Instance of the document
     * @throws EFapsException on error
     */
    void execute(final Parameter _parameter,
                 final Instance _docInst)
        throws EFapsException;

}
