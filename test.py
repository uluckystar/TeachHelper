import aspose.words as aw
import os

def convert_doc_to_docx(doc_path, docx_path):
    # 创建 Document 对象并加载 .doc 文件
    doc = aw.Document(doc_path)
    
    # 保存为 .docx 格式
    doc.save(docx_path, aw.SaveFormat.DOCX)

# 定义输入和输出路径
input_file = "/Users/jiangjiaxing/project/vvv/TeachHelper/uploads/answer/路由与交换技术/2022计科1班-2024-2025-2《路由与交换技术》期末考试--A卷(word)/miniprogram1166115562438104-段志贤-2024-2025-2《路由与交换技术》期末考试--A卷.doc"
output_file = "/Users/jiangjiaxing/project/vvv/TeachHelper/uploads/answer/路由与交换技术/2022计科1班-2024-2025-2《路由与交换技术》期末考试--A卷(word)/converted/miniprogram1166115562438104-段志贤-2024-2025-2《路由与交换技术》期末考试--A卷.docx"

# 调用转换函数
convert_doc_to_docx(input_file, output_file)

print(f"File converted: {output_file}")
