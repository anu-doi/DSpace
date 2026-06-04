/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.embargodatechecker;

import org.apache.commons.cli.ParseException;

/**
 * Extension of {@link EmbargoDateChecker} for CLI.
 *
 * @author Akshay Karthik
 *
 */
public class EmbargoDateCheckerCli extends EmbargoDateChecker {
	
    @Override
    public void setup() throws ParseException {
        super.setup();
        // Check a filename is given
        if (!commandLine.hasOption('f')) {
            throw new ParseException("Required parameter -f missing!");
        }
    }
}
