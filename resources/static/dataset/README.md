# 数据集文件夹

此处为存放数据集的文件夹

## 示例

### 示例1：图片文件数据集

由一些图片文件组成，每一张图片为一个单位数据。

```yaml
format:
  key: $1
  keyType: CHARACTER
  origin: ^(.).png$
  values: null
  valuesSeparator: 或
id: b
md5: 1e2d66107ea61988fc9df230bd6411a9
name: 笔顺
path: data
type: PIC_FILE
```

目录结构如下：

```angular2html
stroke_order/
├── config.yml
└── data
    ├── 吖.png
    ├── 腌.png
    ├── 锕.png
    ├── 阿.png
    ├── 嗄.png
    ├── 啊.png
    ├── 哀.png
    ├── 哎.png
    ├── 唉.png
......
```

### 示例2:文本行数据集

由一个文本文件组成，每一行为一个单位数据。

```yaml
format:
  key: $1
  keyType: UNIQUE_MAX
  origin: "^(.)\t(.+)$"
  values: $2
  valuesSeparator: 或
id: sj
md5: d1db7de6a95558f6338e74f6df213ef8
name: 四角号码-籍合网
path: data/四角号码-籍合网.txt
type: TXT_LINE

```

目录结构如下：

```angular2html
sjhm-jh
├── config.yml
└── data
    └── 四角号码-籍合网.txt
```


### 示例3：图片Zip包数据集

由一个名为 `*.zip` 的zip压缩包组成，压缩包内有一些图片文件（示例为png格式），每个图片文件为一个单位数据。

```yaml
format:
  key: $1
  keyType: UNICODE
  origin: ^(.+).png$
  values: null
  valuesSeparator: 或
id: nu151
md5: 7d1a4de1fa1a83fd2cce52ed15f5b4bd
name: 新·Unicode15.1各地字形
path: data.zip
type: PIC_ZIP
```

目录结构如下（data.zip内有个png文件夹，由于都是zip包内结构，直接写路径+文件名）：
```angular2html
CJK-U151/
├── config.yml
└── data.zip
    ├── png/20000.png
    ├── png/20001.png
......
```


## 配置参数说明

`format` ：数据源格式。配置数据源的处理方式。

`id` ：数据集id。用于查询时指定该数据集，要求全局唯一。

`md5` ：不需要手动填写，由程序自动生成。用于标记当前数据库中的数据集状态，如果数据源发生改变则更新数据库。

`name` ：数据集名称。方便人类阅读。

`path` ：数据源路径。根据不同的数据类型有不同的路径配置方式。如 `TXT_LINE` 类型配置到具体文件； `PIC_FILE` 类型配置到具体目录。

`type` ：数据类型。

### `format` 内的参数

`origin` ：原始数据。
  + 如果是 `TXT_LINE` 类型的数据集，原始数据是文本行，应设置为能匹配所有数据的正则表达式，并且使用括号进行组匹配提取key和values。
  + 如果是 `PIC_FILE` 类型的数据集，原始数据是图片文件，应设置为能匹配所有图片文件名（包含.后缀）的正则表达式，并且使用括号进行组匹配提取key和values。

`key` ：键。放入数据库中的键。从origin的组匹配中提取。

`values` ：值。放入数据库中的值。从origin的组匹配中提取。

`keyType` ：键类型。
  + `TXT_LINE` 类型的数据集可使用：
    - `COMBINE` 重复时不作任何处理。即一个键可对应多个值，取决于数据源中的实际数据。
    - `UNIQUE_MAX` 重复键取最大。越长值越大，一样长则按内码顺序（如英文A-Z）。
        `UNIQUE_MIN` 重复键取最小。
  + `PIC_FILE` 、 `PIC_ZIP` 类型的数据集可使用：
    - `CHARACTER` 配置成此项表示提取的key为字符。
    - `UNICODE` 配置成此项表示提取的key为unicode编码。只支持16进制数字，不含任何其他字符。

`valuesSeparator` ：值连接符。如果一个键对应多个值，将使用此处指定的文本来连接。