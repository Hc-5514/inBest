# Generated by Django 3.2.13 on 2023-09-19 08:28

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('financialData', '0002_auto_20230919_1505'),
    ]

    operations = [
        migrations.AlterField(
            model_name='company',
            name='company_real_industry_code',
            field=models.CharField(max_length=10, null=True),
        ),
    ]
