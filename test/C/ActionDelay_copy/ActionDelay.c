// Code generated by the Lingua Franca compiler from:
// file://Users/shaokai/git/lingua-franca/test/C/src/ActionDelay.lf
#define LOG_LEVEL 4
#define TARGET_FILES_DIRECTORY "/Users/shaokai/git/lingua-franca/test/C/src-gen/ActionDelay"
#include "ctarget.h"
#include "core/threaded/reactor_threaded.c"
#include "core/threaded/scheduler.h"
#include "core/mixed_radix.h"

// Global self struct array.
void *_lf_global_self_structs[4];

int main(int argc, char* argv[]) {
    return lf_reactor_c_main(argc, argv);
}
// =============== START reactor class Source
typedef struct {
    int value;
    bool is_present;
    int num_destinations;
} source_out_t;
typedef struct {
    struct self_base_t base;
    
    
    source_out_t _lf_out;
    int _lf_out_width;
    reaction_t _lf__reaction_0;
    trigger_t _lf__startup;
    reaction_t* _lf__startup_reactions[1];
} source_self_t;
void sourcereaction_function_0(void* instance_args) {
    #pragma GCC diagnostic push
    #pragma GCC diagnostic ignored "-Wunused-variable"
    source_self_t* self = (source_self_t*)instance_args;
    source_out_t* out = &self->_lf_out;
    #pragma GCC diagnostic pop
    SET(out, 1);
        
}
source_self_t* new_Source() {
    source_self_t* self = (source_self_t*)_lf_new_reactor(sizeof(source_self_t));
    self->_lf__reaction_0.number = 0;
    self->_lf__reaction_0.function = sourcereaction_function_0;
    self->_lf__reaction_0.self = self;
    self->_lf__reaction_0.deadline_violation_handler = NULL;
    self->_lf__reaction_0.STP_handler = NULL;
    self->_lf__reaction_0.name = "?";
    self->_lf__reaction_0.mode = NULL;
    self->_lf__startup_reactions[0] = &self->_lf__reaction_0;
    self->_lf__startup.last = NULL;
    self->_lf__startup.reactions = &self->_lf__startup_reactions[0];
    self->_lf__startup.number_of_reactions = 1;
    self->_lf__startup.is_timer = false;
    return self;
}
// =============== END reactor class Source

// =============== START reactor class Sink
typedef struct {
    int value;
    bool is_present;
    int num_destinations;
} sink_in_t;
typedef struct {
    struct self_base_t base;
    
    
    sink_in_t* _lf_in;
    // width of -2 indicates that it is not a multiport.
    int _lf_in_width;
    // Default input (in case it does not get connected)
    sink_in_t _lf_default__in;
    reaction_t _lf__reaction_0;
    trigger_t _lf__in;
    reaction_t* _lf__in_reactions[1];
} sink_self_t;
void sinkreaction_function_0(void* instance_args) {
    #pragma GCC diagnostic push
    #pragma GCC diagnostic ignored "-Wunused-variable"
    sink_self_t* self = (sink_self_t*)instance_args;
    sink_in_t* in = self->_lf_in;
    int in_width = self->_lf_in_width;
    #pragma GCC diagnostic pop
    interval_t elapsed_logical = get_elapsed_logical_time();
    interval_t logical = get_logical_time();
    interval_t physical = get_physical_time();
    printf("Logical, physical, and elapsed logical: %lld %lld %lld.\n", logical, physical, elapsed_logical);
    if (elapsed_logical != MSEC(100)) {
        printf("FAILURE: Expected %lld but got %lld.\n", MSEC(100), elapsed_logical);
        exit(1);
    } else {
        printf("SUCCESS. Elapsed logical time is 100 msec.\n");
    }
        
}
sink_self_t* new_Sink() {
    sink_self_t* self = (sink_self_t*)_lf_new_reactor(sizeof(sink_self_t));
    // Set input by default to an always absent default input.
    self->_lf_in = &self->_lf_default__in;
    self->_lf__reaction_0.number = 0;
    self->_lf__reaction_0.function = sinkreaction_function_0;
    self->_lf__reaction_0.self = self;
    self->_lf__reaction_0.deadline_violation_handler = NULL;
    self->_lf__reaction_0.STP_handler = NULL;
    self->_lf__reaction_0.name = "?";
    self->_lf__reaction_0.mode = NULL;
    self->_lf__in.last = NULL;
    self->_lf__in_reactions[0] = &self->_lf__reaction_0;
    self->_lf__in.reactions = &self->_lf__in_reactions[0];
    self->_lf__in.number_of_reactions = 1;
    self->_lf__in.element_size = sizeof(int);
    return self;
}
// =============== END reactor class Sink

// =============== START reactor class GeneratedDelay
typedef struct {
    int value;
    bool is_present;
    int num_destinations;
} generateddelay_y_in_t;
typedef struct {
    int value;
    bool is_present;
    int num_destinations;
} generateddelay_y_out_t;
typedef struct {
    trigger_t* trigger;
    
    bool is_present;
    bool has_value;
    lf_token_t* token;
    
} generateddelay_act_t;
typedef struct {
    struct self_base_t base;
    
    int y_state;
    generateddelay_act_t _lf_act;
    generateddelay_y_in_t* _lf_y_in;
    // width of -2 indicates that it is not a multiport.
    int _lf_y_in_width;
    // Default input (in case it does not get connected)
    generateddelay_y_in_t _lf_default__y_in;
    generateddelay_y_out_t _lf_y_out;
    int _lf_y_out_width;
    reaction_t _lf__reaction_0;
    reaction_t _lf__reaction_1;
    trigger_t _lf__act;
    reaction_t* _lf__act_reactions[1];
    trigger_t _lf__y_in;
    reaction_t* _lf__y_in_reactions[1];
} generateddelay_self_t;
void generateddelayreaction_function_0(void* instance_args) {
    #pragma GCC diagnostic push
    #pragma GCC diagnostic ignored "-Wunused-variable"
    generateddelay_self_t* self = (generateddelay_self_t*)instance_args;
    generateddelay_y_in_t* y_in = self->_lf_y_in;
    int y_in_width = self->_lf_y_in_width;
    generateddelay_act_t* act = &self->_lf_act;
    #pragma GCC diagnostic pop
    self->y_state = y_in->value;
    schedule(act, MSEC(0));
        
}
void generateddelayreaction_function_1(void* instance_args) {
    #pragma GCC diagnostic push
    #pragma GCC diagnostic ignored "-Wunused-variable"
    generateddelay_self_t* self = (generateddelay_self_t*)instance_args;
    // Expose the action struct as a local variable whose name matches the action name.
    generateddelay_act_t* act = &self->_lf_act;
    // Set the fields of the action struct to match the current trigger.
    act->is_present = (bool)self->_lf__act.status;
    act->has_value = ((self->_lf__act.token) != NULL && (self->_lf__act.token)->value != NULL);
    act->token = (self->_lf__act.token);
    generateddelay_y_out_t* y_out = &self->_lf_y_out;
    #pragma GCC diagnostic pop
    SET(y_out, self->y_state);
        
}
generateddelay_self_t* new_GeneratedDelay() {
    generateddelay_self_t* self = (generateddelay_self_t*)_lf_new_reactor(sizeof(generateddelay_self_t));
    self->_lf_act.trigger = &self->_lf__act;
    // Set input by default to an always absent default input.
    self->_lf_y_in = &self->_lf_default__y_in;
    self->_lf__reaction_0.number = 0;
    self->_lf__reaction_0.function = generateddelayreaction_function_0;
    self->_lf__reaction_0.self = self;
    self->_lf__reaction_0.deadline_violation_handler = NULL;
    self->_lf__reaction_0.STP_handler = NULL;
    self->_lf__reaction_0.name = "?";
    self->_lf__reaction_0.mode = NULL;
    self->_lf__reaction_1.number = 1;
    self->_lf__reaction_1.function = generateddelayreaction_function_1;
    self->_lf__reaction_1.self = self;
    self->_lf__reaction_1.deadline_violation_handler = NULL;
    self->_lf__reaction_1.STP_handler = NULL;
    self->_lf__reaction_1.name = "?";
    self->_lf__reaction_1.mode = NULL;
    self->_lf__act.last = NULL;
    self->_lf__act_reactions[0] = &self->_lf__reaction_1;
    self->_lf__act.reactions = &self->_lf__act_reactions[0];
    self->_lf__act.number_of_reactions = 1;
    self->_lf__act.is_physical = false;
    
    self->_lf__act.element_size = 0;
    self->_lf__y_in.last = NULL;
    self->_lf__y_in_reactions[0] = &self->_lf__reaction_0;
    self->_lf__y_in.reactions = &self->_lf__y_in_reactions[0];
    self->_lf__y_in.number_of_reactions = 1;
    self->_lf__y_in.element_size = sizeof(int);
    return self;
}
// =============== END reactor class GeneratedDelay

// =============== START reactor class ActionDelay
typedef struct {
    struct self_base_t base;
} actiondelay_self_t;
actiondelay_self_t* new_ActionDelay() {
    actiondelay_self_t* self = (actiondelay_self_t*)_lf_new_reactor(sizeof(actiondelay_self_t));
    
    return self;
}
// =============== END reactor class ActionDelay

void _lf_set_default_command_line_options() {}
// Array of pointers to timer triggers to be scheduled in _lf_initialize_timers().
trigger_t** _lf_timer_triggers = NULL;
int _lf_timer_triggers_size = 0;
// Array of pointers to shutdown triggers.
reaction_t** _lf_shutdown_reactions = NULL;
int _lf_shutdown_reactions_size = 0;
trigger_t* _lf_action_for_port(int port_id) {
        return NULL;
}
void _lf_initialize_trigger_objects() {
    // Initialize the _lf_clock
    lf_initialize_clock();
    _lf_tokens_with_ref_count_size = 1;
    _lf_tokens_with_ref_count = (token_present_t*)calloc(1, sizeof(token_present_t));
    if (_lf_tokens_with_ref_count == NULL) error_print_and_exit("Out of memory!");
    // Create the array that will contain pointers to is_present fields to reset on each step.
    _lf_is_present_fields_size = 3;
    _lf_is_present_fields = (bool**)calloc(3, sizeof(bool*));
    if (_lf_is_present_fields == NULL) error_print_and_exit("Out of memory!");
    _lf_is_present_fields_abbreviated = (bool**)calloc(3, sizeof(bool*));
    if (_lf_is_present_fields_abbreviated == NULL) error_print_and_exit("Out of memory!");
    _lf_is_present_fields_abbreviated_size = 0;
    
    _lf_startup_reactions = (reaction_t**)calloc(1, sizeof(reaction_t*));
    _lf_startup_reactions_size = 1;
    int _lf_startup_reactions_count = 0;
    int _lf_shutdown_reactions_count = 0;
    int _lf_timer_triggers_count = 0;
    int _lf_tokens_with_ref_count_count = 0;
    actiondelay_self_t* actiondelay_self[1];
    source_self_t* actiondelay_source_self[1];
    sink_self_t* actiondelay_sink_self[1];
    generateddelay_self_t* actiondelay_g_self[1];
    // Populate the global self struct array
    _lf_global_self_structs[0] = actiondelay_self;
    _lf_global_self_structs[1] = actiondelay_source_self;
    _lf_global_self_structs[2] = actiondelay_sink_self;
    _lf_global_self_structs[3] = actiondelay_g_self;
    // ***** Start initializing ActionDelay of class ActionDelay
    actiondelay_self[0] = new_ActionDelay();
    
    {
        // ***** Start initializing ActionDelay.source of class Source
        actiondelay_source_self[0] = new_Source();
        // width of -2 indicates that it is not a multiport.
        actiondelay_source_self[0]->_lf_out_width = -2;
        _lf_startup_reactions[_lf_startup_reactions_count++] = &actiondelay_source_self[0]->_lf__reaction_0;
        
        //***** End initializing ActionDelay.source
    }
    {
        // ***** Start initializing ActionDelay.sink of class Sink
        actiondelay_sink_self[0] = new_Sink();
        // width of -2 indicates that it is not a multiport.
        actiondelay_sink_self[0]->_lf_in_width = -2;
        
        //***** End initializing ActionDelay.sink
    }
    {
        // ***** Start initializing ActionDelay.g of class GeneratedDelay
        actiondelay_g_self[0] = new_GeneratedDelay();
        // width of -2 indicates that it is not a multiport.
        actiondelay_g_self[0]->_lf_y_out_width = -2;
        // width of -2 indicates that it is not a multiport.
        actiondelay_g_self[0]->_lf_y_in_width = -2;
        { // For scoping
            static int _initial = 0;
            actiondelay_g_self[0]->y_state = _initial;
        } // End scoping.
        // Initializing action ActionDelay.g.act
        actiondelay_g_self[0]->_lf__act.offset = MSEC(100);
        actiondelay_g_self[0]->_lf__act.period = -1;
        actiondelay_g_self[0]->_lf__act.mode = NULL;
        actiondelay_g_self[0]->_lf__act.token = _lf_create_token(0);
        actiondelay_g_self[0]->_lf__act.status = absent;
        _lf_tokens_with_ref_count[_lf_tokens_with_ref_count_count].token = &actiondelay_g_self[0]->_lf__act.token;
        _lf_tokens_with_ref_count[_lf_tokens_with_ref_count_count].status = &actiondelay_g_self[0]->_lf__act.status;
        _lf_tokens_with_ref_count[_lf_tokens_with_ref_count_count++].reset_is_present = true;
        //***** End initializing ActionDelay.g
    }
    //***** End initializing ActionDelay
    
    // **** Start deferred initialize for ActionDelay
    {
        
        
        
        // **** Start deferred initialize for ActionDelay.source
        {
            
            actiondelay_source_self[0]->_lf__reaction_0.name = "ActionDelay.source reaction 0";
            // Total number of outputs (single ports and multiport channels)
            // produced by reaction_0 of ActionDelay.source.
            actiondelay_source_self[0]->_lf__reaction_0.num_outputs = 1;
            // Allocate memory for triggers[] and triggered_sizes[] on the reaction_t
            // struct for this reaction.
            actiondelay_source_self[0]->_lf__reaction_0.triggers = (trigger_t***)_lf_allocate(
                    1, sizeof(trigger_t**),
                    &actiondelay_source_self[0]->base.allocations);
            actiondelay_source_self[0]->_lf__reaction_0.triggered_sizes = (int*)_lf_allocate(
                    1, sizeof(int),
                    &actiondelay_source_self[0]->base.allocations);
            actiondelay_source_self[0]->_lf__reaction_0.output_produced = (bool**)_lf_allocate(
                    1, sizeof(bool*),
                    &actiondelay_source_self[0]->base.allocations);
            {
                int count = 0;
                {
                    actiondelay_source_self[0]->_lf__reaction_0.output_produced[count++] = &actiondelay_source_self[0]->_lf_out.is_present;
                }
            }
            
            // ** End initialization for reaction 0 of ActionDelay.source
            
        }
        // **** End of deferred initialize for ActionDelay.source
        // **** Start deferred initialize for ActionDelay.sink
        {
            
            actiondelay_sink_self[0]->_lf__reaction_0.name = "ActionDelay.sink reaction 0";
            // Total number of outputs (single ports and multiport channels)
            // produced by reaction_0 of ActionDelay.sink.
            actiondelay_sink_self[0]->_lf__reaction_0.num_outputs = 0;
            {
                int count = 0;
            }
            
            // ** End initialization for reaction 0 of ActionDelay.sink
            
        }
        // **** End of deferred initialize for ActionDelay.sink
        // **** Start deferred initialize for ActionDelay.g
        {
            
            actiondelay_g_self[0]->_lf__reaction_0.name = "ActionDelay.g reaction 0";
            // Total number of outputs (single ports and multiport channels)
            // produced by reaction_0 of ActionDelay.g.
            actiondelay_g_self[0]->_lf__reaction_0.num_outputs = 0;
            {
                int count = 0;
            }
            
            // ** End initialization for reaction 0 of ActionDelay.g
            actiondelay_g_self[0]->_lf__reaction_1.name = "ActionDelay.g reaction 1";
            // Total number of outputs (single ports and multiport channels)
            // produced by reaction_1 of ActionDelay.g.
            actiondelay_g_self[0]->_lf__reaction_1.num_outputs = 1;
            // Allocate memory for triggers[] and triggered_sizes[] on the reaction_t
            // struct for this reaction.
            actiondelay_g_self[0]->_lf__reaction_1.triggers = (trigger_t***)_lf_allocate(
                    1, sizeof(trigger_t**),
                    &actiondelay_g_self[0]->base.allocations);
            actiondelay_g_self[0]->_lf__reaction_1.triggered_sizes = (int*)_lf_allocate(
                    1, sizeof(int),
                    &actiondelay_g_self[0]->base.allocations);
            actiondelay_g_self[0]->_lf__reaction_1.output_produced = (bool**)_lf_allocate(
                    1, sizeof(bool*),
                    &actiondelay_g_self[0]->base.allocations);
            {
                int count = 0;
                {
                    actiondelay_g_self[0]->_lf__reaction_1.output_produced[count++] = &actiondelay_g_self[0]->_lf_y_out.is_present;
                }
            }
            
            // ** End initialization for reaction 1 of ActionDelay.g
            
        }
        // **** End of deferred initialize for ActionDelay.g
    }
    // **** End of deferred initialize for ActionDelay
    // **** Start non-nested deferred initialize for ActionDelay
    
    
    
    // **** Start non-nested deferred initialize for ActionDelay.source
    
    // For reference counting, set num_destinations for port ActionDelay.source.out.
    // Iterate over range ActionDelay.source.out(0,1)->[ActionDelay.g.y_in(0,1)].
    {
        int src_runtime = 0; // Runtime index.
        int src_channel = 0; // Channel index.
        int src_bank = 0; // Bank index.
        int range_count = 0;
        actiondelay_source_self[src_runtime]->_lf_out.num_destinations = 1;
    }
    {
        int triggers_index[1] = { 0 }; // Number of bank members with the reaction.
        // Iterate over range ActionDelay.source.out(0,1)->[ActionDelay.g.y_in(0,1)].
        {
            int src_runtime = 0; // Runtime index.
            int src_channel = 0; // Channel index.
            int src_bank = 0; // Bank index.
            int range_count = 0;
            // Reaction 0 of ActionDelay.source triggers 1 downstream reactions
            // through port ActionDelay.source.out.
            actiondelay_source_self[src_runtime]->_lf__reaction_0.triggered_sizes[triggers_index[src_runtime]] = 1;
            // For reaction 0 of ActionDelay.source, allocate an
            // array of trigger pointers for downstream reactions through port ActionDelay.source.out
            trigger_t** trigger_array = (trigger_t**)_lf_allocate(
                    1, sizeof(trigger_t*),
                    &actiondelay_source_self[src_runtime]->base.allocations); 
            actiondelay_source_self[src_runtime]->_lf__reaction_0.triggers[triggers_index[src_runtime]++] = trigger_array;
        }
        for (int i = 0; i < 1; i++) triggers_index[i] = 0;
        // Iterate over ranges ActionDelay.source.out(0,1)->[ActionDelay.g.y_in(0,1)] and ActionDelay.g.y_in(0,1).
        {
            int src_runtime = 0; // Runtime index.
            int src_channel = 0; // Channel index.
            int src_bank = 0; // Bank index.
            // Iterate over range ActionDelay.g.y_in(0,1).
            {
                int dst_runtime = 0; // Runtime index.
                int dst_channel = 0; // Channel index.
                int dst_bank = 0; // Bank index.
                int range_count = 0;
                // Point to destination port ActionDelay.g.y_in's trigger struct.
                actiondelay_source_self[src_runtime]->_lf__reaction_0.triggers[triggers_index[src_runtime] + src_channel][0] = &actiondelay_g_self[dst_runtime]->_lf__y_in;
            }
        }
    }
    
    // **** End of non-nested deferred initialize for ActionDelay.source
    // **** Start non-nested deferred initialize for ActionDelay.sink
    
    
    
    
    // **** End of non-nested deferred initialize for ActionDelay.sink
    // **** Start non-nested deferred initialize for ActionDelay.g
    
    // For reference counting, set num_destinations for port ActionDelay.g.y_out.
    // Iterate over range ActionDelay.g.y_out(0,1)->[ActionDelay.sink.in(0,1)].
    {
        int src_runtime = 0; // Runtime index.
        int src_channel = 0; // Channel index.
        int src_bank = 0; // Bank index.
        int range_count = 0;
        actiondelay_g_self[src_runtime]->_lf_y_out.num_destinations = 1;
    }
    {
        int triggers_index[1] = { 0 }; // Number of bank members with the reaction.
        // Iterate over range ActionDelay.g.y_out(0,1)->[ActionDelay.sink.in(0,1)].
        {
            int src_runtime = 0; // Runtime index.
            int src_channel = 0; // Channel index.
            int src_bank = 0; // Bank index.
            int range_count = 0;
            // Reaction 1 of ActionDelay.g triggers 1 downstream reactions
            // through port ActionDelay.g.y_out.
            actiondelay_g_self[src_runtime]->_lf__reaction_1.triggered_sizes[triggers_index[src_runtime]] = 1;
            // For reaction 1 of ActionDelay.g, allocate an
            // array of trigger pointers for downstream reactions through port ActionDelay.g.y_out
            trigger_t** trigger_array = (trigger_t**)_lf_allocate(
                    1, sizeof(trigger_t*),
                    &actiondelay_g_self[src_runtime]->base.allocations); 
            actiondelay_g_self[src_runtime]->_lf__reaction_1.triggers[triggers_index[src_runtime]++] = trigger_array;
        }
        for (int i = 0; i < 1; i++) triggers_index[i] = 0;
        // Iterate over ranges ActionDelay.g.y_out(0,1)->[ActionDelay.sink.in(0,1)] and ActionDelay.sink.in(0,1).
        {
            int src_runtime = 0; // Runtime index.
            int src_channel = 0; // Channel index.
            int src_bank = 0; // Bank index.
            // Iterate over range ActionDelay.sink.in(0,1).
            {
                int dst_runtime = 0; // Runtime index.
                int dst_channel = 0; // Channel index.
                int dst_bank = 0; // Bank index.
                int range_count = 0;
                // Point to destination port ActionDelay.sink.in's trigger struct.
                actiondelay_g_self[src_runtime]->_lf__reaction_1.triggers[triggers_index[src_runtime] + src_channel][0] = &actiondelay_sink_self[dst_runtime]->_lf__in;
            }
        }
    }
    
    // **** End of non-nested deferred initialize for ActionDelay.g
    // **** End of non-nested deferred initialize for ActionDelay
    // Connect inputs and outputs for reactor ActionDelay.
    // Connect inputs and outputs for reactor ActionDelay.source.
    // Connect ActionDelay.source.out(0,1)->[ActionDelay.g.y_in(0,1)] to port ActionDelay.g.y_in(0,1)
    // Iterate over ranges ActionDelay.source.out(0,1)->[ActionDelay.g.y_in(0,1)] and ActionDelay.g.y_in(0,1).
    {
        int src_runtime = 0; // Runtime index.
        int src_channel = 0; // Channel index.
        int src_bank = 0; // Bank index.
        // Iterate over range ActionDelay.g.y_in(0,1).
        {
            int dst_runtime = 0; // Runtime index.
            int dst_channel = 0; // Channel index.
            int dst_bank = 0; // Bank index.
            int range_count = 0;
            actiondelay_g_self[dst_runtime]->_lf_y_in = (generateddelay_y_in_t*)&actiondelay_source_self[src_runtime]->_lf_out;
        }
    }
    // Connect inputs and outputs for reactor ActionDelay.sink.
    // Connect inputs and outputs for reactor ActionDelay.g.
    // Connect ActionDelay.g.y_out(0,1)->[ActionDelay.sink.in(0,1)] to port ActionDelay.sink.in(0,1)
    // Iterate over ranges ActionDelay.g.y_out(0,1)->[ActionDelay.sink.in(0,1)] and ActionDelay.sink.in(0,1).
    {
        int src_runtime = 0; // Runtime index.
        int src_channel = 0; // Channel index.
        int src_bank = 0; // Bank index.
        // Iterate over range ActionDelay.sink.in(0,1).
        {
            int dst_runtime = 0; // Runtime index.
            int dst_channel = 0; // Channel index.
            int dst_bank = 0; // Bank index.
            int range_count = 0;
            actiondelay_sink_self[dst_runtime]->_lf_in = (sink_in_t*)&actiondelay_g_self[src_runtime]->_lf_y_out;
        }
    }
    {
    }
    {
    }
    {
        {
            // Add action ActionDelay.g.act to array of is_present fields.
            _lf_is_present_fields[0] 
                    = &actiondelay_g_self[0]->_lf_act.is_present;
        }
    }
    {
        int count = 0;
        {
            // Add port ActionDelay.source.out to array of is_present fields.
            _lf_is_present_fields[1 + count] = &actiondelay_source_self[0]->_lf_out.is_present;
            count++;
        }
    }
    {
        int count = 0;
        {
            // Add port ActionDelay.g.y_out to array of is_present fields.
            _lf_is_present_fields[2 + count] = &actiondelay_g_self[0]->_lf_y_out.is_present;
            count++;
        }
    }
    
    // Set reaction priorities for ReactorInstance ActionDelay
    {
        
        // Set reaction priorities for ReactorInstance ActionDelay.source
        {
            actiondelay_source_self[0]->_lf__reaction_0.chain_id = 1;
            // index is the OR of level 0 and 
            // deadline 140737488355327 shifted left 16 bits.
            actiondelay_source_self[0]->_lf__reaction_0.index = 0x7fffffffffff0000LL;
        }
        
        
        // Set reaction priorities for ReactorInstance ActionDelay.sink
        {
            actiondelay_sink_self[0]->_lf__reaction_0.chain_id = 1;
            // index is the OR of level 3 and 
            // deadline 140737488355327 shifted left 16 bits.
            actiondelay_sink_self[0]->_lf__reaction_0.index = 0x7fffffffffff0003LL;
        }
        
        
        // Set reaction priorities for ReactorInstance ActionDelay.g
        {
            actiondelay_g_self[0]->_lf__reaction_0.chain_id = 1;
            // index is the OR of level 1 and 
            // deadline 140737488355327 shifted left 16 bits.
            actiondelay_g_self[0]->_lf__reaction_0.index = 0x7fffffffffff0001LL;
            actiondelay_g_self[0]->_lf__reaction_1.chain_id = 1;
            // index is the OR of level 2 and 
            // deadline 140737488355327 shifted left 16 bits.
            actiondelay_g_self[0]->_lf__reaction_1.index = 0x7fffffffffff0002LL;
        }
        
    }
    
    // Initialize the scheduler
    size_t num_reactions_per_level[4] = 
        {1, 1, 1, 1};
    sched_params_t sched_params = (sched_params_t) {
                            .num_reactions_per_level = &num_reactions_per_level[0],
                            .num_reactions_per_level_size = (size_t) 4};
    lf_sched_init(
        (size_t)_lf_number_of_workers,
        &sched_params
    );
}
void _lf_trigger_startup_reactions() {
    for (int i = 0; i < _lf_startup_reactions_size; i++) {
        if (_lf_startup_reactions[i] != NULL) {
            #ifdef MODAL_REACTORS
            if (!_lf_mode_is_active(_lf_startup_reactions[i]->mode)) {
                // Mode is not active. Remember to trigger startup when the mode
                // becomes active.
                _lf_startup_reactions[i]->mode->should_trigger_startup = true;
                continue;
            }
            #endif
            _lf_trigger_reaction(_lf_startup_reactions[i], -1);
        }
    }
}
void _lf_initialize_timers() {

}
void logical_tag_complete(tag_t tag_to_send) {

}
bool _lf_trigger_shutdown_reactions() {

    // Return true if there are shutdown reactions.
    return (_lf_shutdown_reactions_size > 0);
}
void terminate_execution() {}
