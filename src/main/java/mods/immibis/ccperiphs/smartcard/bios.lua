function os.version()
	return "SmartBIOS 49.1.0"
end

os.pullEventRaw = coroutine.yield
function os.pullEvent(filter)
	local evt, a, b, c, d, e = coroutine.yield(filter)
	if evt == "terminate" then
		print("Terminated")
		error()
	end
	return evt, a, b, c, d, e
end

function sleep(time)
	local timer = os.startTimer(time)
	repeat
		local e, p = os.pullEvent("timer")
	until p == timer
end

-- For debugging output
write = term.write

function print(...)
	local string = ""
	for _,v in ipairs{...} do
		string = string .. tostring(v)
	end
	write(string .. "\n")
end



-- Remove irrelevant native APIs
term, http, peripheral = nil
os.computerID = nil
os.getComputerID = nil
os.getComputerLabel = nil
os.setComputerLabel = nil



-- Debugging input
function read()
	local e, p = os.pullEvent("sc_debug_input")
	return p
end




function loadfile(fn)
	local f = fs.open(fn, "r")
	if not f then
		return nil, "File not found"
	else
		local code = f.readAll()
		f.close()
		return loadstring(code)
	end
end

function dofile(fn)
	local fn = assert(loadfile(fn))
	setfenv(fn, getfenv(2))
	fn()
end

local _G = _G

-- from computer bios
function os.run( _tEnv, _sPath, ... )
    local tArgs = { ... }
    local fnFile, err = loadfile( _sPath )
    if fnFile then
        local tEnv = _tEnv
        setmetatable( tEnv, { __index = _G } )
        setfenv( fnFile, tEnv )
        local ok, err = pcall( function()
        	fnFile( unpack( tArgs ) )
        end )
        if not ok then
        	if err and err ~= "" then
	        	print( err )
	        end
        	return false
        end
        return true
    end
    if err and err ~= "" then
		print( err )
	end
    return false
end

local nativegetmetatable = getmetatable
local nativetype = type
local nativeerror = error
function getmetatable( _t )
	if nativetype( _t ) == "string" then
		nativeerror( "Attempt to access string metatable" )
		return nil
	end
	return nativegetmetatable( _t )
end

-- embedded device; doesn't have global table protection

--[[
local tAPIsLoading = {}
function os.loadAPI( _sPath )
	local sName = fs.getName( _sPath )
	if tAPIsLoading[sName] == true then
		print( "API "..sName.." is already being loaded" )
		return false
	end
	tAPIsLoading[sName] = true
		
	local tEnv = {}
	setmetatable( tEnv, { __index = _G } )
	local fnAPI, err = loadfile( _sPath )
	if fnAPI then
		setfenv( fnAPI, tEnv )
		fnAPI()
	else
		print( err )
        tAPIsLoading[sName] = nil
		return false
	end
	
	local tAPI = {}
	for k,v in pairs( tEnv ) do
		tAPI[k] =  v
	end
	protect( tAPI )
	
	bProtected = false
	_G[sName] = tAPI
	bProtected = true
	
	tAPIsLoading[sName] = nil
	return true
end

function os.unloadAPI( _sName )
	if _sName ~= "_G" and type(_G[_sName]) == "table" then
		bProtected = false
		_G[_sName] = nil
		bProtected = true
	end
end]]

os.sleep = sleep

debug = {}
debug.recv = read
debug.send = write

do
	local send, recv, loadstring, pcall, tostring = debug.send, debug.recv, loadstring, pcall, tostring
	function debug.console()
		send("Debug console opened.")
		while true do
			local code = recv()
			local fn, err = loadstring(code)
			if not fn then
				send("compile error: "..tostring(err, "debug"))
			else
				local ok, err = pcall(fn)
				if not ok then
					send("runtime error: "..tostring(err))
				else
					send("result: "..tostring(err))
				end
			end
		end
	end
end

os.shutdown = nil
os.reboot = nil

local yield, pcall, write, tostring, console = coroutine.yield, pcall, write, tostring, debug.console

if fs.exists("/startup") then
	dofile("/startup")
end

local ok, err = pcall(console)
if not ok then
	write(tostring(err))
end
while true do yield() end

