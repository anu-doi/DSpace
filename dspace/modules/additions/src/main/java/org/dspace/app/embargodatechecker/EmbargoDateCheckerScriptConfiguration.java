/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.embargodatechecker;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;


/**
 * Script configuration for {@link EmbargoDateChecker}.
 *
 * @author Akshay Karthik
 *
 * @param  <T> the {@link EmbargoDateChecker} type
 */
public class EmbargoDateCheckerScriptConfiguration<T extends EmbargoDateChecker> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("u", "uuid", true, "target uuid of community or collection");
            options.getOption("u").setType(String.class);
            
//            options.addOption("a", "all", false, "run for the whole site");

            options.addOption("d", "duration", true, "number of days");
            options.getOption("d").setType(String.class);
            
            options.addOption("s", "start-date", true, "start date of range, format - yyyy-MM-dd eg: 2026-01-31");
            options.getOption("s").setType(String.class);
            
            options.addOption("e", "end-date", true, "end date of range, format - yyyy-MM-dd eg: 2026-01-31");
            options.getOption("e").setType(String.class);
            
            options.addOption("h", "help", false, "help");
            super.options = options;

        }
        return options;
    }

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    /**
     * Generic setter for the dspaceRunnableClass
     *
     * @param dspaceRunnableClass The dspaceRunnableClass to be set on this
     *                            EmbargoDateCheckerScriptConfiguration
     */
    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

}
