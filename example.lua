---@meta EXAMPLE comments marked with ---@example can be ignored, they are not of any interest to the parser.
---these meta comments can have multiple lines, so becareful when parsing them

---@class ExampleClass : table Classes can also have meta comments
---these comments can have multiple lines as well as field description
---as show below
---@field private string field1 this is a private field of type string
---@field public number? field2 this is an optional field that is public and of type number
---@field boolean|number field3 this field can be either boolean or number
---@field fun(param1: string, param2: number): nil method1 this is a method that takes
---a string and a number and returns nil, also these field descriptions can have multiple lines
ExampleClass = {}

---@type string this is a static field of type string for the class
---this can have a multiline comment,
---all multiline comments can have multiple lines, not just two.
---This field should also be added to the class itself, but marked as static
ExampleClass.someField = "example"

---@type number this is a local field and not part of the class.
---In fact it should be completely ignored by the parser
local someField = 42

function myFunction(param1)
    -- this is a function with a parameter, but no meta comments
    -- this function should not be ignored, the parameter should
    -- be of type "any" and have no description
    -- if there is a return statement at the end of the function
    -- the return type should also be "any" with no description
    -- it doesn't belong to any class
    return param1
end

function ExampleClass.staticMethod(param1, param2)
    -- this is a static method of the ExampleClass class
    -- it has two parameters, but no meta comments
    -- so both parameters should be of type "any" with no description
    -- the return type should also be "any" with no description
    -- also important to notice, the dot syntax indicates that this is a static method
    -- a none-static method would use the colon syntax instead
end

---This is a documented function with meta comments
---This is a second line of the description for this function.
---@param param1 string this is the first parameter of type string for this function
---@param param2 ExampleClass this is the second parameter of type ExampleClass for this function
function ExampleClass:method1(param1, param2)
    -- this is a method of the ExampleClass class
    -- it has two parameters, but no meta comments
    -- so both parameters should be of type "any" with no description
    -- the return type should also be "any" with no description
    -- also important to notice, the colon syntax indicates that this is a none-static method
    -- a static method would use the dot syntax instead

    ---@cast param1 string

    -- the cast line above can be ignored. In general, if there is something that is not recognized, just ignore it
end

---@enum ENUM this is an enum, which sould be represented as class
---in the documentation
ENUM = {
    -- this is the first value of the enum, and should be added as field to the class with out any description, but
    -- of type any, except if a type is explicit given like in the next example
    VLAUE1 = 1,

    ---@type number this is the second value of the enum, with an explicit type
    VALUE2 = 2,
}
