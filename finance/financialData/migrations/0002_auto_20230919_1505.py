# Generated by Django 3.2.13 on 2023-09-19 06:05

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('financialData', '0001_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='company',
            name='company_real_industry_code',
            field=models.IntegerField(default=None, null=True),
        ),
        migrations.AlterField(
            model_name='company',
            name='company_industry_code',
            field=models.IntegerField(default=None, null=True),
        ),
    ]
