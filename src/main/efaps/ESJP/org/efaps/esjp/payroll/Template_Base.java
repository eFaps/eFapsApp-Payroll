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
package org.efaps.esjp.payroll;

import java.util.ArrayList;
import java.util.List;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.payroll.rules.AbstractRule;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * 
 */
@EFapsUUID("da8d1940-4dda-43b2-976d-c67dbd5cb8e6")
@EFapsApplication("eFapsApp-Payroll")
public abstract class Template_Base
    extends AbstractCommon
{

    protected static List<? extends AbstractRule<?>> getRules4Template(final Parameter _parameter,
                                                                       final Instance _templateInst)
        throws EFapsException
    {
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.RuleAbstract);
        attrQueryBldr.addWhereAttrEqValue(CIPayroll.RuleAbstract.StatusAbstract,
                        Status.find(CIPayroll.RuleStatus.Active));

        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Template2Rule);
        queryBldr.addWhereAttrInQuery(CIPayroll.Template2Rule.ToLink,
                        attrQueryBldr.getAttributeQuery(CIPayroll.RuleAbstract.ID));
        queryBldr.addWhereAttrEqValue(CIPayroll.Template2Rule.FromLink, _templateInst);
        queryBldr.addOrderByAttributeAsc(CIPayroll.Template2Rule.Sequence);

        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder ruleInst = SelectBuilder.get().linkto(CIPayroll.Template2Rule.ToLink).instance();
        multi.addSelect(ruleInst);
        multi.setEnforceSorted(true);
        multi.execute();

        final List<Instance> ruleInsts = new ArrayList<>();
        while (multi.next()) {
            ruleInsts.add(multi.<Instance>getSelect(ruleInst));
        }
        return AbstractRule.getRules(ruleInsts.toArray(new Instance[ruleInsts
                        .size()]));
    }
}
