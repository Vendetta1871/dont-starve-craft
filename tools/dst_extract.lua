--[[
Extracts DST data tables from the shipped Lua scripts into JSON so the port cites
numbers instead of retyping them:
  tuning.json  - every TUNING value after Tune() (numbers, strings, booleans, tables of them)
  recipes.json - Recipe2/Recipe/DeconstructRecipe: ingredients, tech level, config
  crafting.json - crafting menu filters (tab order, recipe lists) and TECH levels
  loot.json    - SetSharedLootTable tables declared at the top level of prefabs/*.lua

Usage (LuaJIT, DST is Lua 5.1):
  tools/dst_extract.sh                       # unpacks scripts.zip and runs this
  luajit tools/dst_extract.lua <scripts-dir> <out-dir>

The scripts run in a permissive sandbox: any global the extraction does not need
resolves to an inert stub, so engine-only calls are harmless.
]]
local SRC, OUT = arg[1], arg[2]
assert(SRC and OUT, "usage: luajit dst_extract.lua <scripts-dir> <out-dir>")

-- Inert stub: indexing, calling and arithmetic all yield another stub.
local stub_mt = {}
local function is_stub(v) return type(v) == "table" and getmetatable(v) == stub_mt end
local function stub() return setmetatable({}, stub_mt) end
stub_mt.__index = function() return stub() end
stub_mt.__call = function() return stub() end
for _, m in ipairs({"__add", "__sub", "__mul", "__div", "__mod", "__pow", "__unm", "__concat"}) do
    stub_mt[m] = function() return stub() end
end
stub_mt.__lt = function() return false end
stub_mt.__le = function() return false end

local G = {}
setmetatable(G, {__index = function(_, k)
    local v = _G[k]
    if v ~= nil and k ~= "require" and k ~= "arg" then return v end
    return stub()
end})
G._G = G

local loaded = {}
local function run(path, env)
    local f, err = loadfile(path)
    if not f then return false, err end
    setfenv(f, env or G)
    return pcall(f)
end
G.require = function(name)
    if loaded[name] ~= nil then return loaded[name] end
    local path = SRC .. "/" .. name:gsub("%.", "/") .. ".lua"
    local ok, res = run(path)
    if not ok then
        loaded[name] = stub()
    else
        loaded[name] = res == nil and true or res
    end
    return loaded[name]
end

for _, name in ipairs({"class", "util", "vector3", "constants", "techtree", "tuning"}) do
    G.require(name)
end
assert(type(rawget(G, "Tune")) == "function", "tuning.lua did not load")
local ok, err = pcall(G.Tune)
assert(ok, "Tune() failed: " .. tostring(err))

-- JSON writer with sorted keys; functions and stubs are left out.
local function encode(v, indent, seen)
    local t = type(v)
    if t == "number" then
        if v ~= v or v == math.huge or v == -math.huge then return '"' .. tostring(v) .. '"' end
        if v == math.floor(v) and math.abs(v) < 1e15 then return string.format("%d", v) end
        return string.format("%.10g", v)
    elseif t == "string" then
        return '"' .. v:gsub('[%c"\\]', function(c)
            return ({['"'] = '\\"', ['\\'] = '\\\\', ['\n'] = '\\n', ['\t'] = '\\t'})[c]
                or string.format("\\u%04x", c:byte())
        end) .. '"'
    elseif t == "boolean" then
        return tostring(v)
    elseif t == "table" and not is_stub(v) then
        if seen[v] then return nil end
        seen[v] = true
        local keys, n = {}, #v
        local is_array = n > 0
        for k in pairs(v) do
            if is_array and (type(k) ~= "number" or k > n) then is_array = false end
            keys[#keys + 1] = k
        end
        local pad = string.rep("  ", indent + 1)
        local parts = {}
        if is_array then
            for i = 1, n do
                local e = encode(v[i], indent + 1, seen)
                if e then parts[#parts + 1] = pad .. e end
            end
        else
            table.sort(keys, function(a, b) return tostring(a) < tostring(b) end)
            for _, k in ipairs(keys) do
                local e = encode(v[k], indent + 1, seen)
                if e then parts[#parts + 1] = pad .. encode(tostring(k), 0, {}) .. ": " .. e end
            end
        end
        seen[v] = nil
        if #parts == 0 then return is_array and "[]" or "{}" end
        local close = string.rep("  ", indent)
        if is_array then return "[\n" .. table.concat(parts, ",\n") .. "\n" .. close .. "]" end
        return "{\n" .. table.concat(parts, ",\n") .. "\n" .. close .. "}"
    end
    return nil
end
local function write(name, value)
    local f = assert(io.open(OUT .. "/" .. name, "w"))
    f:write(encode(value, 0, {}) or "null", "\n")
    f:close()
end

write("tuning.json", rawget(G, "TUNING"))

-- Recipes: record the constructor arguments instead of building Recipe objects
-- (the returned table takes field assignments such as .testfn).
local recipes = {}
local function tech_name(tech)
    for k, v in pairs(rawget(G, "TECH") or {}) do
        if v == tech then return k end
    end
    return nil
end
G.Ingredient = function(kind, amount)
    return {item = kind, amount = amount}
end
G.Recipe2 = function(name, ingredients, tech, config)
    recipes[name] = {ingredients = ingredients, tech = tech_name(tech), config = config}
    return {}
end
G.Recipe = function(name, ingredients, tab, level, more)
    recipes[name] = {ingredients = ingredients, tech = tech_name(level),
        config = type(more) == "table" and more or nil}
    return {}
end
G.DeconstructRecipe = function(name, ingredients, config)
    recipes[name] = {deconstruct = true, ingredients = ingredients, config = config}
    return {}
end
G.AddRecipeToFilter = function() end
loaded["recipe"] = true -- keep the recording constructors above
local rok, rerr = run(SRC .. "/recipes.lua")
assert(rok, "recipes.lua failed: " .. tostring(rerr))
write("recipes.json", recipes)

-- Crafting menu: the filter tabs in their on-screen order with their recipe lists,
-- and the tech level each TECH constant requires (zero levels left out).
local frok, frerr = run(SRC .. "/recipes_filter.lua")
assert(frok, "recipes_filter.lua failed: " .. tostring(frerr))
local filters = {}
for _, def in ipairs(rawget(G, "CRAFTING_FILTER_DEFS") or {}) do
    local list = rawget(G, "CRAFTING_FILTERS")[def.name].recipes
    filters[#filters + 1] = {name = def.name, recipes = type(list) == "table" and list or {}}
end
local tech = {}
for name, levels in pairs(rawget(G, "TECH") or {}) do
    local t = {}
    for tree, level in pairs(levels) do
        if type(level) == "number" and level > 0 then t[tree] = level end
    end
    tech[name] = t
end
write("crafting.json", {filters = filters, tech = tech})

-- Shared loot tables from the prefab files.
local loot, failed = {}, {}
G.SetSharedLootTable = function(name, entries)
    local out = {}
    for _, e in ipairs(entries or {}) do
        out[#out + 1] = {item = e[1], chance = e[2]}
    end
    loot[name] = out
end
local p = io.popen('ls "' .. SRC .. '/prefabs"')
for file in p:lines() do
    if file:match("%.lua$") then
        local env = setmetatable({}, {__index = G})
        local fok = run(SRC .. "/prefabs/" .. file, env)
        if not fok then failed[#failed + 1] = file end
    end
end
p:close()
write("loot.json", loot)

local function count(t) local n = 0 for _ in pairs(t) do n = n + 1 end return n end
print(string.format("tuning: %d keys, recipes: %d, loot tables: %d (prefab files that errored: %d)",
    count(rawget(G, "TUNING")), count(recipes), count(loot), #failed))
