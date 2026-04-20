def calculator(operation, operand1, operand2):
    if operation == 'add':
        return operand1 + operand2
    elif operation == 'subtract':
        return operand1 - operand2
    elif operation == 'multiply':
        return operand1 * operand2
    elif operation == 'divide':
        if operand2 == 0:
            raise ValueError('Cannot divide by zero')
        return operand1 / operand2
    else:
        raise ValueError(f'Invalid operation: {operation}')


## 定义工具：按照一定格式组织 Json 类型数据，后续使用 Claude 时传入
## json 中的每个参数都非常重要，需确保正确填写而且需要认真定义描述，description越详细，大模型会越精确调用函数
calculator_tool = {
    "name": "calculator",
    ## description非常重要，描述越详细，大模型执行越准确
    "description": "一个执行两个数进行加减乘除运算的计算器",
    "input_schema": {
        "type": "object",
        ## properties中需要配置参数所需要的参数与描述，要与calculator函数的参数一致
        "properties": {
            "operation": {
                "type": "string",
                ## 枚举类型
                "enum": ["add", "subtract", "multiply", "divide"],
                "description": "两数要执行的运算类型"
            },
            "operand1": {
                "type": "number",
                "description": "运算的第一个数字"
            },
            "operand2": {
                "type": "number",
                "description": "运算的第二个数字"
            }
        },
        "required": ["operation", "operand1", "operand2"]
    },
}

client = anthropic.Anthropic(
    api_key=ANTHROPIC_API_KEY,
)

messages=[{"role": "user", "content": "我有 23只鸡，但有 2 只飞走了，问剩下多少只？"}]
response = client.messages.create(
    model="claude-4.5-haiku",
    system="你可以使用工具，仅在用户输入的内容是两数计算相关内容，否则就正常回复。不要给用户返回与计算器相关的任何内容和上下文",
    messages=messages,
    max_tokens=400,
    ## 告诉 claude 必要时调用工具
    tools=[calculator_tool]
)

# 打印内容
print(response)

# if response.stop_reson == 'tool_use':
#     # 获取 Claude 提供的工具名称和输入
#     tool_use = response.content[-1]
#     tool_name = tool_use.name
#     tool_input = tool_use.input
#
#     if tool_name == 'calculator':
#         print("Claude使用了 calculator 工具")
#         # 获取工具相应的参数
#         operation = tool_input["operation"]
#         operand1 = tool_input["operand1"]
#         operand2 = tool_input["operand2"]
#
#         try:
#             result = calculator(operation, operand1, operand2)
#             print("计算结果为:,", result)
#         except ValueError as e:
#             print(f"错误: {str(e)}")
# elif response.stop_reson == "end_turn":
#     print("Claude没有使用工具")
#     print("Claude回复：", response.content[0].text)


if response.stop_reson == 'tool_use':
    # 在原始消息中添加助手角色，内容为大模型返回的内容
    messages.append({"role": "assistant", "content": response.content})

    # 获取 Claude 提供的工具名称和输入
    tool_use = response.content[-1]
    tool_name = tool_use.name
    tool_input = tool_use.input

    if tool_name == 'calculator':
        print("Claude使用了 calculator 工具")
        # 获取工具相应的参数
        operation = tool_input["operation"]
        operand1 = tool_input["operand1"]
        operand2 = tool_input["operand2"]

        try:
            result = calculator(operation, operand1, operand2)
            tool_result = {
                "role":"user",
                "content":[
                    {
                        "type": "tool_result",
                        "tool_use_id": tool_use.id,
                        "content":"结果: " + str(result)
                    }
                ]
            }

            messages.append(tool_result)

            # 将组织好的 message 再次提交给 Claude
            response = client.messages.create(
                model="claude-4.5-haiku",
                messages=messages,
                max_tokens=400,
                tools=[calculator_tool]
            )
            print("Claude 最终回复:")
            print(response.content[0].text)
        except ValueError as e:
            print(f"错误: {str(e)}")
elif response.stop_reson == "end_turn":
    print("Claude没有使用工具")
    print("Claude回复：", response.content[0].text)


##没有使用返回:
# Message(
#   id='ms9_01FLZcBiBDscPEFxFntEuTMj'，
#   content=[
#       Text1OCK(teX三!很他歉,您的问题没在与计复器功能相关，天空的服色通常及监色，这足由于地球大气层中的氮久和包对别光的放射滋康的。天空的色会恨服各种因素面发生变化，比如天气、时间、地理)
#   model='cLaude-3-haiku-20240307'，
#   role='assistant',
#   stop_reason='end_turn',
#   stop sequence-llne, FYpe 'message', usageUsage(input tokens-63?, output. tkens:12?, cache creation, input. tokens0, cache read. input tokenso))

##使用工具返回:
# Message(
#   id='msg_015mN59mjBesi0ndfN1eo57y',
#   content=[
#       TextBLock(text='好的，让我来计算一下:'，type='text')，
#       TooLUseBLock(
#           id='toolu_01GZHpDdQyKEehLGuPMRudDo',
#           input={'operand1':23,'operand2':2,'operation':'subtract'},
#           name='calculator',
#           type='tool_use'
#   model='claude-3-haiku-20240307',
#   role='assistant',
#   stop_reason='tool_use',
#   stop_sequence=None
#   type='message',
#   usage=Usage(input tokens=655, output tokens-102, cache_creation_input tokens=0, cache-read input tokens=0)
