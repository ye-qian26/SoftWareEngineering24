declare namespace API {
  type AppAddRequest = {
    initPrompt?: string
  }

  type AppAdminUpdateRequest = {
    id?: string | number
    appName?: string
    cover?: string
    priority?: number
  }

  type AppDeployRequest = {
    appId?: string | number
  }

  type AppQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string | number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    priority?: number
    userId?: string | number
  }

  type AppUpdateRequest = {
    id?: string | number
    appName?: string
  }

  type AppVO = {
    id?: string | number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: string | number
    createTime?: string
    updateTime?: string
    user?: UserVO
  }

  type BaseResponseAppVO = {
    code?: number
    data?: AppVO
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: string | number
    message?: string
  }

  type BaseResponsePageAppVO = {
    code?: number
    data?: PageAppVO
    message?: string
  }

  type BaseResponsePageChatHistory = {
    code?: number
    data?: PageChatHistory
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type ChatHistory = {
    id?: string | number
    message?: string
    messageType?: string
    appId?: string | number
    userId?: string | number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type ChatHistoryQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string | number
    message?: string
    messageType?: string
    appId?: string | number
    userId?: string | number
    lastCreateTime?: string
  }

  type chatToGenCodeParams = {
    appId: string | number
    message: string
  }

  type DeleteRequest = {
    id?: string | number
  }

  type downloadAppCodeParams = {
    appId: string | number
  }

  type getAppVOByIdByAdminParams = {
    id: string | number
  }

  type getAppVOByIdParams = {
    id: string | number
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type listAppChatHistoryParams = {
    appId: string | number
    pageSize?: number
    lastCreateTime?: string
  }

  type LoginUserVO = {
    id?: string | number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
    updateTime?: string
  }

  type PageAppVO = {
    records?: AppVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageChatHistory = {
    records?: ChatHistory[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type ServerSentEventString = true

  type serveStaticResourceParams = {
    deployKey: string
  }

  type User = {
    id?: string | number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }
}
