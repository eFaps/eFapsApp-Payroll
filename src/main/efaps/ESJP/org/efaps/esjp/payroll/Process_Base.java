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
package org.efaps.esjp.payroll;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.payroll.util.Payroll;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * The Class Process_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("29d33306-c2bf-4f58-a369-c888ac5ca715")
@EFapsApplication("eFapsApp-Payroll")
public abstract class Process_Base
    extends CommonDocument
{

    /**
     * Creates the.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
       throws EFapsException
    {
        final Create create = new Create() {
            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final Insert _insert)
                                               throws EFapsException
            {
                super.add2basicInsert(_parameter, _insert);
                _insert.add(CIPayroll.ProcessAbstract.Name, getDocName4Create(_parameter));
            }
        };
        return create.execute(_parameter);
    }

    /**
     * Execute process.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return executeProcess(final Parameter _parameter)
        throws EFapsException
    {
        final Instance processInst = _parameter.getInstance();
        final PrintQuery print = new PrintQuery(processInst);
        final SelectBuilder sel = SelectBuilder.get()
                        .linkto(CIPayroll.ProcessAbstract.ProcessDefinitionAbstractLink);
        final SelectBuilder selTemplInst = new SelectBuilder(sel)
                        .linkto(CIPayroll.ProcessDefinitionAbstract.TemplateLink)
                        .instance();
        final SelectBuilder selEmplGrpInst =  new SelectBuilder(sel)
                        .linkto(CIPayroll.ProcessDefinitionAbstract.EmployeeGroupLink)
                        .instance();
        final SelectBuilder selDocTypeInst =  new SelectBuilder(sel)
                        .linkto(CIPayroll.ProcessDefinitionAbstract.DocType)
                        .instance();
        print.addSelect(selTemplInst, selEmplGrpInst, selDocTypeInst);
        print.addAttribute(CIPayroll.ProcessAbstract.StartDate, CIPayroll.ProcessAbstract.EndDate);
        print.execute();

        final Instance templInst = print.getSelect(selTemplInst);
        final Instance emplGrpInst = print.getSelect(selEmplGrpInst);
        final Instance docTypeInst = print.getSelect(selDocTypeInst);

        final DateTime startDate = print.getAttribute(CIPayroll.ProcessAbstract.StartDate);
        final DateTime endDate = print.getAttribute(CIPayroll.ProcessAbstract.EndDate);

        if (templInst.getType().isCIType(CIPayroll.TemplatePayslip)) {

            final Parameter parameter = ParameterUtil.clone(_parameter);
            ParameterUtil.setProperty(parameter, "JasperConfig", Payroll.getSysConfig().getUUID().toString());
            ParameterUtil.setProperty(parameter, "JasperConfigReport", Payroll.PAYSLIPJASPERREPORT.getKey());
            ParameterUtil.setProperty(parameter, "JasperConfigMime", Payroll.PAYSLIPMIME.getKey());

            final Payslip payslip = new Payslip() {
                @Override
                protected Type getType4DocCreate(final Parameter _parameter)
                    throws EFapsException
                {
                    return CIPayroll.Payslip.getType();
                }
            };

            final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Employee2EmployeeGroup);
            attrQueryBldr.addWhereAttrEqValue(CIPayroll.Employee2EmployeeGroup.ToLink, emplGrpInst);

            final QueryBuilder queryBldr = new QueryBuilder(CIHumanResource.Employee);
            queryBldr.addWhereAttrEqValue(CIHumanResource.Employee.Status,
                            Status.find(CIHumanResource.EmployeeStatus.Worker));
            queryBldr.addWhereAttrInQuery(CIHumanResource.Employee.ID,
                            attrQueryBldr.getAttributeQuery(CIPayroll.Employee2EmployeeGroup.FromLink));
            final InstanceQuery query = queryBldr.getQuery();
            query.execute();
            while (query.next()) {
                parameter.put(ParameterValues.INSTANCE, processInst);
                payslip.create(parameter, templInst, query.getCurrentValue(), startDate, endDate, docTypeInst);
            }
        } else if (templInst.getType().isCIType(CIPayroll.TemplateAdvance)) {

            final Parameter parameter = ParameterUtil.clone(_parameter);
            ParameterUtil.setProperty(parameter, "JasperConfig", Payroll.getSysConfig().getUUID().toString());
            ParameterUtil.setProperty(parameter, "JasperConfigReport", Payroll.ADVANCEJASPERREPORT.getKey());
            ParameterUtil.setProperty(parameter, "JasperConfigMime", Payroll.ADVANCEMIME.getKey());

            final Advance adavance = new Advance() {
                @Override
                protected Type getType4DocCreate(final Parameter _parameter)
                    throws EFapsException
                {
                    return CIPayroll.Advance.getType();
                }
            };;

            final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Employee2EmployeeGroup);
            attrQueryBldr.addWhereAttrEqValue(CIPayroll.Employee2EmployeeGroup.ToLink, emplGrpInst);

            final QueryBuilder queryBldr = new QueryBuilder(CIHumanResource.Employee);
            queryBldr.addWhereAttrEqValue(CIHumanResource.Employee.Status,
                            Status.find(CIHumanResource.EmployeeStatus.Worker));
            queryBldr.addWhereAttrInQuery(CIHumanResource.Employee.ID,
                            attrQueryBldr.getAttributeQuery(CIPayroll.Employee2EmployeeGroup.FromLink));
            final InstanceQuery query = queryBldr.getQuery();
            query.execute();
            while (query.next()) {
                parameter.put(ParameterValues.INSTANCE, processInst);
                adavance.create(parameter, templInst, query.getCurrentValue(), startDate, docTypeInst);
            }
        }
        final Update update = new Update(processInst);
        update.add(CIPayroll.ProcessAbstract.StatusAbstract, Status.find(CIPayroll.ProcessStatus.Closed));
        update.executeWithoutAccessCheck();
        return new Return();
    }
}
