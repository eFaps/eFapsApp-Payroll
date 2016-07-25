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
 * Revision:        $Rev: 295 $
 * Last Changed:    $Date: 2011-06-06 15:37:52 -0500 (lun, 06 jun 2011) $
 * Last Changed By: $Author: Jorge Cueva $
 */
package org.efaps.esjp.payroll.reports;

import java.math.BigDecimal;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: WorkOrderCalibrateDataSource.java 268 2011-04-29 17:10:40Z Jorge Cueva $
 */
@EFapsUUID("275b47ff-d9ac-4c68-a809-f2f7b2d165f7")
@EFapsApplication("eFapsApp-Payroll")
public abstract class AbstractReports_Base
{
    /**
     * Logger for this classes.
     */
    protected final static Logger LOG = LoggerFactory.getLogger(AbstractReports_Base.class);

    /**
     * Definitions for Columns.
     */
    public interface Column
    {
        /**
         * Getter method for the instance variable {@link #key}.
         *
         * @return value of instance variable {@link #key}
         */
        String getKey();
        /**
         * Getter method for the instance variable {@link #length}.
         *
         * @return value of instance variable {@link #length}
         */
        Integer getLength();
        /**
         * @return
         */
        boolean isOptional();
        /**
         * @return
         */
        String getDefaultVal();

        Integer getDecimalLength();
    }

    protected String getName4TextFile(final String _format,
                                      final DateTime _date)
        throws EFapsException
    {
        final String year = "" + _date.getYear();
        final String month = _date.getMonthOfYear() < 10
                        ? "0" + _date.getMonthOfYear() : "" + _date.getMonthOfYear();

        final String ruc = ERP.COMPANYTAX.get();
        final String name = _format.concat(year).concat(month).concat(ruc);
        return name;
    }

    protected String getCharacterValue(final Object _value,
                                       final Column _column)
        throws EFapsException
    {
        String value = String.valueOf(_value);

        if (value.length() > _column.getLength()) {
            value = value.substring(0, _column.getLength());
        }
        return value;
    }

    protected String getNumberValue(final Object _value,
                                    final Column _column)
        throws EFapsException
    {
        String valStr = "";
        if (_value instanceof Integer) {
            valStr = ((Integer) _value).toString();
        } else if (_value instanceof BigDecimal) {
            try {
                final BigDecimal valTmp = (BigDecimal) _value;
                if (valTmp.subtract(new BigDecimal(valTmp.intValue())).compareTo(BigDecimal.ZERO) != 0) {
                    valStr = ((BigDecimal) _value).setScale(_column.getDecimalLength(), BigDecimal.ROUND_HALF_UP)
                                    .toString();
                } else {
                    valStr = "" + ((BigDecimal) _value).intValue();
                }
            } catch (final ArithmeticException e) {
                throw new EFapsException(AbstractReports_Base.class, "execute.IOException", e);
            }
        } else if (_value instanceof String) {
            valStr = ((String) _value).replace(",", ".");
        }

        return valStr;
    }

    protected String getSeparator() {
        return "|";
    }

    public Column get0Column() {
        return new Column() {

            @Override
            public String getKey()
            {
                return null;
            }

            @Override
            public Integer getLength()
            {
                return 0;
            }

            @Override
            public Integer getDecimalLength()
            {
                return null;
            }

            @Override
            public boolean isOptional()
            {
                return false;
            }

            @Override
            public String getDefaultVal()
            {
                return null;
            }
        };
    }
}
