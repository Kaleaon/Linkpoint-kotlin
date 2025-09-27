# SecondLife Message Template
# Original from: https://github.com/secondlife/viewer/blob/main/scripts/messages/message_template.msg
# Translated to modern format with type safety and validation

# Version info
version 2.0
trust AgentID SessionID

# Basic ping/connectivity messages
{
StartPingCheck Low NotTrusted Unencoded
    {
        PingID Single
            { PingID    U32 }
            { OldestUnacked U32 }
    }
}

{
CompletePingCheck Low NotTrusted Unencoded
    {
        PingID Single
            { PingID    U32 }
    }
}

# Login system messages
{
LoginRequest Low NotTrusted Unencoded
    {
        CircuitInfo Single
            { IP        IPADDR }
            { Port      IPPORT }
    }
    {
        LoginInfo Single
            { UserName  Variable 1 }
            { Password  Variable 1 }
            { Start     Variable 1 }
            { ClientVersion Variable 1 }
            { Platform  Variable 1 }
            { Options   Variable 1 }
        }
    }
}

{
LoginReply Low NotTrusted Unencoded
    {
        AgentData Single
            { AgentID           LLUUID }
            { SessionID         LLUUID }
            { SecureSessionID   LLUUID }
            { AgentAccess       S8 }
            { BuddyRights       U32 }
            { InventoryHost     Variable 1 }
            { SeedCapability    Variable 1 }
        }
    }
    {
        RegionData Single
            { RegionX           U32 }
            { RegionY           U32 }
            { SimulatorIP       IPADDR }
            { SimulatorPort     IPPORT }
            { SeedCapability    Variable 1 }
        }
    }
}

# Chat and messaging
{
ChatFromViewer Low NotTrusted Unencoded
    {
        AgentData Single
            { AgentID       LLUUID }
            { SessionID     LLUUID }
        }
    }
    {
        ChatData Single
            { Message       Variable 1 }
            { Type          U8 }
            { Channel       S32 }
        }
    }
}

{
ChatFromSimulator Low NotTrusted Unencoded
    {
        ChatData Single
            { FromName      Variable 1 }
            { SourceID      LLUUID }
            { OwnerID       LLUUID }
            { SourceType    U8 }
            { ChatType      U8 }
            { Audible       U8 }
            { Position      LLVector3 }
            { Message       Variable 2 }
        }
    }
}

# Region handshake
{
RegionHandshake Low NotTrusted Unencoded
    {
        RegionInfo Single
            { RegionFlags           U32 }
            { SimAccess             U8 }
            { SimName               Variable 1 }
            { SimOwner              LLUUID }
            { IsEstateManager       BOOL }
            { WaterHeight           F32 }
            { BillableFactor        F32 }
            { CacheID               LLUUID }
        }
    }
}

{
RegionHandshakeReply Low NotTrusted Unencoded
    {
        AgentData Single
            { AgentID       LLUUID }
            { SessionID     LLUUID }
        }
    }
    {
        RegionInfo Single
            { Flags         U32 }
        }
    }
}

# Agent updates
{
AgentUpdate High NotTrusted Unencoded
    {
        AgentData Single
            { AgentID       LLUUID }
            { SessionID     LLUUID }
            { BodyRotation  LLQuaternion }
            { HeadRotation  LLQuaternion }
            { State         U8 }
            { CameraCenter  LLVector3 }
            { CameraAtAxis  LLVector3 }
            { CameraLeftAxis LLVector3 }
            { CameraUpAxis  LLVector3 }
            { Far           F32 }
            { ControlFlags  U32 }
            { Flags         U8 }
        }
    }
}

# Generic message template for extensibility
{
GenericMessage Low NotTrusted Unencoded
    {
        AgentData Single
            { AgentID       LLUUID }
            { SessionID     LLUUID }
            { TransactionID LLUUID }
        }
    }
    {
        MethodData Single
            { Method        Variable 1 }
            { Invoice       LLUUID }
        }
    }
    {
        ParamList Variable
            { Parameter     Variable 1 }
        }
    }
}

# End of message template