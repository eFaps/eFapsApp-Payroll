/*
 * Copyright 2003 - 2014 The eFaps Team
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


package org.efaps.esjp.payroll.rules;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jexl2.JexlContext;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;



/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("0fbe877d-a876-41d6-9882-a072a680e023")
@EFapsRevision("$Rev$")
public abstract class IncomeTax5_Base
{
    protected final static String KEYS4PAYMENT = "IncomeTax5PaymentKeys";
    protected final static String KEYS4EXTRA = "IncomeTax5ExtraKeys";
    protected final static String KEYS4TAX = "IncomeTax5TaxKeys";

    public final static Map<Integer, Integer> MONTH2DIVID = new HashMap<Integer, Integer>();
    static {
        IncomeTax5_Base.MONTH2DIVID.put(1, 12);
        IncomeTax5_Base.MONTH2DIVID.put(2, 12);
        IncomeTax5_Base.MONTH2DIVID.put(3, 12);
        IncomeTax5_Base.MONTH2DIVID.put(4, 9);
        IncomeTax5_Base.MONTH2DIVID.put(5, 8);
        IncomeTax5_Base.MONTH2DIVID.put(6, 8);
        IncomeTax5_Base.MONTH2DIVID.put(7, 8);
        IncomeTax5_Base.MONTH2DIVID.put(8, 5);
        IncomeTax5_Base.MONTH2DIVID.put(9, 4);
        IncomeTax5_Base.MONTH2DIVID.put(10, 4);
        IncomeTax5_Base.MONTH2DIVID.put(11, 4);
        IncomeTax5_Base.MONTH2DIVID.put(12, 1);
    }
    public final static Map<Integer, Integer> MONTH2SUBSTRACT = new HashMap<Integer, Integer>();
    static {
        IncomeTax5_Base.MONTH2SUBSTRACT.put(4, 3);
        IncomeTax5_Base.MONTH2SUBSTRACT.put(5, 4);
        IncomeTax5_Base.MONTH2SUBSTRACT.put(6, 4);
        IncomeTax5_Base.MONTH2SUBSTRACT.put(7, 4);
        IncomeTax5_Base.MONTH2SUBSTRACT.put(8, 7);
        IncomeTax5_Base.MONTH2SUBSTRACT.put(9, 8);
        IncomeTax5_Base.MONTH2SUBSTRACT.put(10, 8);
        IncomeTax5_Base.MONTH2SUBSTRACT.put(11, 8);
        IncomeTax5_Base.MONTH2SUBSTRACT.put(12, 11);
    }

    public final static Map<Integer, Integer> MONTH2MULITPLY = new HashMap<Integer, Integer>();
    static {
        IncomeTax5_Base.MONTH2MULITPLY.put(1, 14);
        IncomeTax5_Base.MONTH2MULITPLY.put(2, 13);
        IncomeTax5_Base.MONTH2MULITPLY.put(3, 12);
        IncomeTax5_Base.MONTH2MULITPLY.put(4, 11);
        IncomeTax5_Base.MONTH2MULITPLY.put(5, 10);
        IncomeTax5_Base.MONTH2MULITPLY.put(6, 9);
        IncomeTax5_Base.MONTH2MULITPLY.put(7, 7);
        IncomeTax5_Base.MONTH2MULITPLY.put(8, 6);
        IncomeTax5_Base.MONTH2MULITPLY.put(9, 5);
        IncomeTax5_Base.MONTH2MULITPLY.put(10, 4);
        IncomeTax5_Base.MONTH2MULITPLY.put(11, 3);
        IncomeTax5_Base.MONTH2MULITPLY.put(12, 1);
    }

    public BigDecimal get(final JexlContext _context)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final Instance employeeInst = (Instance) _context.get(AbstractParameter.PARAKEY4EMPLOYINST);
        final DateTime date = (DateTime) _context.get(AbstractParameter.PARAKEY4DATE);
        final Object uitObj = _context.get("UIT");

        if (employeeInst != null && employeeInst.isValid()) {
            final BigDecimal uit = getBigDecimal(uitObj);
            final BigDecimal base = getCurrent(_context, IncomeTax5.KEYS4PAYMENT);
            final BigDecimal extra = getCurrent(_context, IncomeTax5.KEYS4EXTRA);

            final BigDecimal payed = getPrevious(_context, IncomeTax5.KEYS4PAYMENT, date.getMonthOfYear() - 1);
            final BigDecimal extraPayed = getPrevious(_context, IncomeTax5.KEYS4EXTRA, date.getMonthOfYear() - 1);

            final int currentMonth = date.getMonthOfYear();

            BigDecimal yearAmountBruto = base.multiply(new BigDecimal(MONTH2MULITPLY
                            .get(currentMonth)));
            yearAmountBruto = yearAmountBruto.add(payed).add(extra).add(extraPayed);







            final BigDecimal yearAmountNeto = yearAmountBruto.subtract(uit.multiply(new BigDecimal(7)));
            BigDecimal restNetto = yearAmountNeto;

            final BigDecimal first = uit.multiply(new BigDecimal(27));
            final BigDecimal second = uit.multiply(new BigDecimal(54));
            BigDecimal yearTax = BigDecimal.ZERO;

            if (yearAmountNeto.compareTo(BigDecimal.ZERO) > 0) {
                final BigDecimal amount = yearAmountNeto.compareTo(first) < 1 ? yearAmountNeto : first;
                yearTax = yearTax.add(amount.multiply(new BigDecimal(15).divide(new BigDecimal(100))));
                restNetto = restNetto.subtract(amount);
            }

            if (restNetto.compareTo(BigDecimal.ZERO) > 0) {
                final BigDecimal amount = second.compareTo(yearAmountNeto) < 1
                                ? second.subtract(first) : yearAmountNeto.subtract(first);
                yearTax = yearTax.add(amount.multiply(new BigDecimal(21).divide(new BigDecimal(100))));
                restNetto = restNetto.subtract(amount);
            }

            if (restNetto.compareTo(BigDecimal.ZERO) > 0) {
                yearTax = yearTax.add(yearAmountNeto.subtract(second)
                                .multiply(new BigDecimal(30).divide(new BigDecimal(100))));
            }

            if (yearTax.compareTo(BigDecimal.ZERO) > 0) {
                final BigDecimal taxPayed;
                if (MONTH2SUBSTRACT.containsKey(currentMonth)) {
                    taxPayed = getPrevious(_context, IncomeTax5.KEYS4TAX, MONTH2SUBSTRACT.get(currentMonth));
                } else {
                    taxPayed = BigDecimal.ZERO;
                }
                ret = yearTax.subtract(taxPayed).divide(new BigDecimal(MONTH2DIVID.get(currentMonth)),
                                BigDecimal.ROUND_HALF_UP);
            }
        }
        return ret;
    }

    private BigDecimal getBigDecimal(final Object _object)
    {
        BigDecimal ret = BigDecimal.ZERO;
        if (_object != null) {
            if (_object instanceof BigDecimal) {
                ret = (BigDecimal) _object;
            } else if (_object instanceof Double) {
                ret = new BigDecimal((Double) _object);
            } else if (_object instanceof BigInteger) {
                ret = new BigDecimal((BigInteger) _object);
            } else if (_object instanceof Long) {
                ret = new BigDecimal((Long) _object);
            } else if (_object instanceof Integer) {
                ret = new BigDecimal((Integer) _object);
            } else if (_object instanceof Float) {
                ret = new BigDecimal((Float) _object);
            }
        }
        return ret;
    }

    private BigDecimal getCurrent(final JexlContext _context,
                                  final String _key)
    {
        BigDecimal ret = BigDecimal.ZERO;
        if (_context.has(_key)) {
            final String[] keys = (String[]) _context.get(_key);
            for (final String key : keys) {
                String keyTmp;
                if (Character.isDigit(key.charAt(0))) {
                    keyTmp = "$" + key;
                } else {
                    keyTmp = key;
                }
                ret = ret.add(getBigDecimal(_context.get(keyTmp)));
            }
        } else {
            //TODO logging
        }
        return ret;
    }


    private BigDecimal getPrevious(final JexlContext _context,
                                   final String _key,
                                   final Integer _month)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;

        if (_context.has(_key)) {
            final String[] keys = (String[]) _context.get(_key);
            final DataFunctions data = new DataFunctions(_context);
            ret = data.getAnualMonth(_month, keys);
        } else {
            final DataFunctions data = new DataFunctions(_context);
            ret = data.getAnualMonth(_month, _key);
        }
        return ret;
    }
}
