/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.embargodatechecker;

import java.io.OutputStream;

import org.apache.commons.cli.Options;

/**
 * Extension of {@link EmbargoDateCheckerScriptConfiguration} for CLI.
 *
 * @author Akshay Karthik
 *
 */
public class EmbargoDateCheckerCliScriptConfiguration extends EmbargoDateCheckerScriptConfiguration<EmbargoDateCheckerCli> {

    @Override
    public Options getOptions() {
        options = super.getOptions();
        options.addOption("f", "file", true, "destination where you want file written");
        options.getOption("f").setType(OutputStream .class);
        options.getOption("f").setRequired(true);
        return options;
    }
}
