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

package org.efaps.esjp.payroll.listener;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.accounting.listener.IOnDocumentInfo;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("057895b4-cde8-477f-8d78-1479109aeb5c")
@EFapsApplication("efapsApp-Payroll")
public abstract class OnDocumentInfo_Base
    implements IOnDocumentInfo
{

    @Override
    public Map<String, BigDecimal> getKey2Amount(final Instance _docInst)
        throws EFapsException
    {
        final Map<String, BigDecimal> ret = new HashMap<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink, _docInst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.PositionAbstract.Key, CIPayroll.PositionAbstract.Amount);
        multi.execute();
        while (multi.next()) {
            final String key = multi.getAttribute(CIPayroll.PositionAbstract.Key);
            final BigDecimal amount = multi.getAttribute(CIPayroll.PositionAbstract.Amount);
            final BigDecimal current = ret.containsKey(key) ? ret.get(key) : BigDecimal.ZERO;
            ret.put(key, current.add(amount));
        }
        return ret;
    }

    @Override
    public int getWeight()
    {
        return 0;
    }
}
