/** Instance of a timer. */

/*************
Copyright (c) 2019, The University of California at Berkeley.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
***************/

package org.lflang.generator;

import org.lflang.ASTUtils;
import org.lflang.TimeValue;
import org.lflang.lf.Parameter;
import org.lflang.lf.TimeUnit;
import org.lflang.lf.Timer;

/**
 * Instance of a timer.
 * 
 * @author{Marten Lohstroh <marten@berkeley.edu>}
 * @author{Edward A. Lee <eal@berkeley.edu>}
 */
public class TimerInstance extends TriggerInstance<Timer> {
    
  	/**
	 * Create a new timer instance.
	 * If the definition is null, then this is a startup timer.
	 * @param definition The AST definition, or null for startup.
	 * @param parent The parent reactor.
	 */
    public TimerInstance(Timer definition, ReactorInstance parent) {
		super(definition, parent);
        if (parent == null) {
            throw new InvalidSourceException("Cannot create an TimerInstance with no parent.");
        }
        if (definition != null) {
            if (definition.getOffset() != null) {
                if (definition.getOffset().getParameter() != null) {
                    Parameter parm = definition.getOffset().getParameter();
                    this.offset = ASTUtils.getTimeValue(parent.initialParameterValue(parm).get(0));
                } else {
                    this.offset = ASTUtils.getTimeValue(definition.getOffset());
                }
            }
            if (definition.getPeriod() != null) {
                if (definition.getPeriod().getParameter() != null) {
                    Parameter parm = definition.getPeriod().getParameter();
                    this.period = ASTUtils.getTimeValue(parent.initialParameterValue(parm).get(0));
                } else {
                    this.period = ASTUtils.getTimeValue(definition.getPeriod());
                }
            }
        }
    }
    
    //////////////////////////////////////////////////////
    //// Public fields.

    /** The global default for offset. */
    public static TimeValue DEFAULT_OFFSET = new TimeValue(0, TimeUnit.NONE);
    
    /** The global default for period. */
    public static TimeValue DEFAULT_PERIOD = new TimeValue(0, TimeUnit.NONE);
    
    //////////////////////////////////////////////////////
    //// Public methods.

    /**
     * Get the value of the offset parameter.
     */
    public TimeValue getOffset() {
        return offset;
    }

    /**
     * Get the value of the offset parameter.
     */
    public TimeValue getPeriod() {
        return period;
    }

    //////////////////////////////////////////////////////
    //// Protected fields.

    protected TimeValue offset = DEFAULT_OFFSET;
    
    protected TimeValue period = DEFAULT_PERIOD;
}
