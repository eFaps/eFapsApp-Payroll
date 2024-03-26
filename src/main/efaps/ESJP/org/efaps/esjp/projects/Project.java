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
package org.efaps.esjp.projects;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsNoUpdate;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;

/**
 * This class is only used in case that the Projects App is not installed to be
 * able to compile the classes.
 * @author The eFaps Team
 * @version $Id: Project.java 5526 2010-09-10 14:17:54Z miguel.a.aranya $
 */
@EFapsUUID("7bc8c88f-64c3-402d-aed9-1ad0d93f5437")
@EFapsApplication("eFapsApp-Payroll")
@EFapsNoUpdate
public class Project
{
    public StringBuilder getProjectData(final Parameter _parameter,
                                        final Instance _currentValue)
    {
        // PLACEHOLDER ONLY
        return null;
    }
}
